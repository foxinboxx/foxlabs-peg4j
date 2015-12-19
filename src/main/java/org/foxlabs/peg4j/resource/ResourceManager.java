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
    
    public static final String RESOURCE_DIRECTORY = ResourceHelper.getResourcePath(ResourceManager.class);
    
    private static final MessageBundle MESSAGE_BUNDLE = MessageBundle.getInstance(RESOURCE_DIRECTORY + "/messages");
    private static final MessageBundle PROBLEM_BUNDLE = MessageBundle.getInstance(RESOURCE_DIRECTORY + "/problems");
    private static final MessageBundle CLI_BUNDLE = MessageBundle.getInstance(RESOURCE_DIRECTORY + "/cli");
    
    private static final String[] PRODUCT_INFO = ResourceHelper.readManifestAttributes("Peg4j-Name", "Peg4j-Version", "Peg4j-URL");
    
    private static String grammarTextTemplate = null;
    private static String grammarJavaTemplate = null;
    private static String grammarHtmlTemplate = null;
    private static String grammarDefaultCssTheme = null;
    private static String grammarJavascriptCode = null;
    
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
            grammarTextTemplate = ResourceHelper.readTextResource(RESOURCE_DIRECTORY + "/text.template");
        }
        return grammarTextTemplate;
    }
    
    public static String getGrammarJavaTemplate() {
        if (grammarJavaTemplate == null) {
            grammarJavaTemplate = ResourceHelper.readTextResource(RESOURCE_DIRECTORY + "/java.template");
        }
        return grammarJavaTemplate;
    }
    
    public static String getGrammarHtmlTemplate() {
        if (grammarHtmlTemplate == null) {
            grammarHtmlTemplate = ResourceHelper.readTextResource(RESOURCE_DIRECTORY + "/html.template");
        }
        return grammarHtmlTemplate;
    }
    
    public static String getGrammarDefaultCssTheme() {
        if (grammarDefaultCssTheme == null) {
            grammarDefaultCssTheme = ResourceHelper.readTextResource(RESOURCE_DIRECTORY + "/grammar.css");
        }
        return grammarDefaultCssTheme;
    }
    
    public static String getGrammarJavascriptCode() {
        if (grammarJavascriptCode == null) {
            grammarJavascriptCode = ResourceHelper.readTextResource(RESOURCE_DIRECTORY + "/grammar.js");
        }
        return grammarJavascriptCode;
    }
    
    public static String getMessage(String key) {
        return MESSAGE_BUNDLE.get(key);
    }
    
    public static String formatMessage(String key, Object... arguments) {
        return MESSAGE_BUNDLE.format(key, arguments);
    }
    
    public static String formatProblemMessage(String key, String... attributes) {
        return PROBLEM_BUNDLE.format(key, (Object[]) attributes);
    }
    
    public static String formatCliMessage(String key, Object... arguments) {
        return CLI_BUNDLE.format(key, arguments);
    }
    
    public static String getCopyrightInfo() {
        return formatMessage("copyright.generatedMessage", getProductName(), getProductVersion());
    }
    
}
