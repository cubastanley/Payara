/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.ejb.rest.client;

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.security.AccessController.doPrivileged;
import static java.util.Collections.emptyList;
import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.glassfish.jersey.internal.util.ReflectionHelper.getClassLoaderPA;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public final class EjbRestProxyFactory implements InvocationHandler {

    private final WebTarget target;
    private final MultivaluedMap<String, Object> headers;
    private final List<Cookie> cookies;
    private final String lookup;
    private final Map<String, Object> jndiOptions;

    private static final MultivaluedMap<String, Object> EMPTY_MULTI_MAP = new MultivaluedHashMap<>();

    public static <C> C newProxy(Class<C> remoteBusinessInterface, WebTarget target, String lookup, Map<String, Object> jndiOptions) {
        return newProxy(remoteBusinessInterface, target, EMPTY_MULTI_MAP, emptyList(), lookup, jndiOptions);
    }

    @SuppressWarnings("unchecked")
    public static <C> C newProxy(Class<C> remoteBusinessInterface, WebTarget target, MultivaluedMap<String, Object> headers, List<Cookie> cookies, String lookup, Map<String, Object> jndiOptions) {
        return (C) 
            newProxyInstance(doPrivileged(getClassLoaderPA(remoteBusinessInterface)),
                new Class[] { remoteBusinessInterface },
                new EjbRestProxyFactory(addPathFromClass(remoteBusinessInterface, target), headers, cookies, lookup, jndiOptions));
    }

    private EjbRestProxyFactory(WebTarget target, MultivaluedMap<String, Object> headers, List<Cookie> cookies, String lookup, Map<String, Object> jndiOptions) {
        this.target = target;
        this.headers = headers;
        this.cookies = cookies;
        this.lookup = lookup;
        this.jndiOptions = jndiOptions;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] argValues) throws Throwable {
        
        // Check for methods we should not proxy first
        
        if (argValues == null && method.getName().equals("toString")) {
            return toString();
        }

        if (argValues == null && method.getName().equals("hashCode")) {
            // unique instance in the JVM, and no need to override
            return hashCode();
        }

        if (argValues != null && argValues.length == 1 && method.getName().equals("equals")) {
            // unique instance in the JVM, and no need to override
            return equals(argValues[0]);
        }
        
        // Valid method, do invoke it remotely
        return doRemoteInvoke(proxy, method, argValues);
    }
    
    public Object doRemoteInvoke(Object proxy, Method method, Object[] argValues) throws Throwable {
        // HTTP method name; we're always using POST to invoke remote EJBs
        String httpMethod = POST;
        
        // The bare payload being sent 
        Map<String, Object> payload = new HashMap<>();
        payload.put("lookup", lookup);
        payload.put("method", method.getName());
        payload.put("argTypes", method.getParameterTypes());
        payload.put("argValues", argValues == null? new Object[0] : argValues);
        
        if (jndiOptions.containsKey(SECURITY_PRINCIPAL)) {
            payload.put(SECURITY_PRINCIPAL, base64Encode(jndiOptions.get(SECURITY_PRINCIPAL)));
        }
        
        if (jndiOptions.containsKey(SECURITY_CREDENTIALS)) {
            payload.put(SECURITY_CREDENTIALS, base64Encode(jndiOptions.get(SECURITY_CREDENTIALS)));
        }
        
        // Payload wrapped as entity so it'll be encoded in JSON
        Entity<?> entity = Entity.entity(payload, APPLICATION_JSON);
        
        // Response type
        Class<?> responseType = method.getReturnType();
        GenericType<?> responseGenericType = new GenericType<>(method.getGenericReturnType());

        // Create a new UriBuilder appending the name from the method
        WebTarget newTarget = addPathFromMethod(method, target);
        
        // Start request
        Invocation.Builder builder = newTarget.request();
                
        // Set optional headers and cookies    
        builder.headers(new MultivaluedHashMap<String, Object>(this.headers));
        for (Cookie cookie : new LinkedList<>(this.cookies)) {
            builder = builder.cookie(cookie);
        }
        
        // Call remote server
        
        if (responseType.isAssignableFrom(CompletionStage.class)) {
            
            // Reactive call - the actual response type is T from CompletionStage<T>
            return builder.rx().method(httpMethod, entity, getResponseParameterizedType(method, responseGenericType));
        } else if (responseType.isAssignableFrom(Future.class)) {
            
            // Asynchronous call - the actual response type is T from Future<T>
            return builder.async().method(httpMethod, entity, getResponseParameterizedType(method, responseGenericType));
        }
        
        // Synchronous call           
        return builder.method(httpMethod, entity, responseGenericType);
    }
    
    private GenericType<?> getResponseParameterizedType(Method method, GenericType<?> responseGenericType) {
        if (method.getGenericReturnType() instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) method.getGenericReturnType();
            
            return new GenericType<>(parameterizedType.getActualTypeArguments()[0]);
        }
        
        return responseGenericType;
    }
    
    private static String base64Encode(Object input) {
        return Base64.getEncoder().encodeToString(input.toString().getBytes());
    }

    private static WebTarget addPathFromMethod(Method method, WebTarget target) {
        return target.path(method.getName());
    }

    private static WebTarget addPathFromClass(Class<?> clazz, WebTarget target) {
        return target.path(clazz.getSimpleName());
    }

    @Override
    public String toString() {
        return target.toString();
    }

}
