/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.glassfish.web.deployment.descriptor;

import jakarta.servlet.descriptor.TaglibDescriptor;
import java.io.Serializable;

/**
 * This descriptor represent the information about a tag library used in a
 * web application.
 *
 * @author Danny Coward
 */

public class TagLibConfigurationDescriptor
        implements Serializable, TaglibDescriptor {

    private String uri;
    private String location;

    /**
     * Default constructor.
     */
    public TagLibConfigurationDescriptor() {
    }

    /**
     * Construct a tag library configuration with the given location and URI.
     * @param the URI.
     * @param the location.
     */
    public TagLibConfigurationDescriptor(String uri, String location) {
	this.uri = uri;
	this.location = location;
    }

    /**
     * Sets the URI of this tag lib.
     * @param the URI of the tag library.
     */
    public void setTagLibURI(String uri) {
        this.uri = uri;
    }

    /**
     * Return the URI of this tag lib.
     * @return the URI of the tag library.
     */
    public String getTagLibURI() {
	if (this.uri == null) {
	    this.uri = "";
	}
	return this.uri;
    }

    public String getTaglibURI() {
        return getTagLibURI();
    }

    /**
     * Describe the location of the tag library file.
     * @param the location of the tag library.
     */
    public void setTagLibLocation(String location) {
        this.location = location;
    }

    /**
     * Describes the location of the tag library file.
     * @return the location of the tag library.
     */
    public String getTagLibLocation() {
	if (this.location == null) {
	    this.location = "";
	}
	return this.location;
    }

    public String getTaglibLocation() {
        return getTagLibLocation();
    }

    /**
     * Return a formatted String representing my state.
     */
    public void print(StringBuilder toStringBuilder) {
	toStringBuilder.append("TGLIB: ").append(uri).append(", ").append(location);
    }

}
