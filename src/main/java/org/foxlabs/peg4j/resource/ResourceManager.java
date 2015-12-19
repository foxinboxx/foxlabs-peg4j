/* 
 * Copyright (C) 2014 FoxLabs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.foxlabs.peg4j.resource;

import java.util.Date;

import org.foxlabs.util.resource.MessageBundle;
import org.foxlabs.util.resource.ResourceHelper;

public abstract class ResourceManager {
    
    private static final String[] PRODUCT_INFO = ResourceHelper.readManifestAttributes("Peg4j-Name", "Peg4j-Version", "Peg4j-URL");
    public static final String RESOURCE_DIRECTORY = ResourceHelper.getResourcePath(ResourceManager.class);
    
    private static final MessageBundle RUNTIME_BUNDLE = MessageBundle.getInstance(RESOURCE_DIRECTORY + "/runtime-messages");
    private static final MessageBundle PROBLEM_BUNDLE = MessageBundle.getInstance(RESOURCE_DIRECTORY + "/problem-messages");
    private static final MessageBundle CLI_BUNDLE = MessageBundle.getInstance(RESOURCE_DIRECTORY + "/cli-messages");
    
    private static String grammarTextTemplate = null;
    private static String grammarJavaTemplate = null;
    private static String grammarHtmlTemplate = null;
    
    private ResourceManager() {
        super();
    }
    
    public static String getProductName() {
        if (PRODUCT_INFO[0] == null) {
            PRODUCT_INFO[0] = "Peg4j";
        }
        return PRODUCT_INFO[0];
    }
    
    public static String getProductVersion() {
        if (PRODUCT_INFO[1] == null) {
            PRODUCT_INFO[1] = new Date().toString();
        }
        return PRODUCT_INFO[1];
    }
    
    public static String getProductURL() {
        if (PRODUCT_INFO[2] == null) {
            PRODUCT_INFO[2] = "http://foxlabs.org/p/peg4j";
        }
        return PRODUCT_INFO[2];
    }
    
    public static String getGrammarTextTemplate() {
        if (grammarTextTemplate == null) {
            grammarTextTemplate = ResourceHelper.readTextResource(
                    RESOURCE_DIRECTORY + "/template/grammar-template.peg4j");
        }
        return grammarTextTemplate;
    }
    
    public static String getGrammarJavaTemplate() {
        if (grammarJavaTemplate == null) {
            grammarJavaTemplate = ResourceHelper.readTextResource(
                    RESOURCE_DIRECTORY + "/template/grammar-template.java");
        }
        return grammarJavaTemplate;
    }
    
    public static String getGrammarHtmlTemplate() {
        if (grammarHtmlTemplate == null) {
            grammarHtmlTemplate = ResourceHelper.readTextResource(
                    RESOURCE_DIRECTORY + "/template/grammar-template.html");
        }
        return grammarHtmlTemplate;
    }
    
    public static String formatRuntimeMessage(String key, Object... arguments) {
        return RUNTIME_BUNDLE.format(key, arguments);
    }
    
    public static String formatProblemMessage(String key, String... attributes) {
        return PROBLEM_BUNDLE.format(key, (Object[]) attributes);
    }
    
    public static String formatCliMessage(String key, Object... arguments) {
        return CLI_BUNDLE.format(key, arguments);
    }
    
}
