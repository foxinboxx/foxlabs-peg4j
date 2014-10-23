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

import java.net.URL;

import org.foxlabs.peg4j.CommandLine;

import org.foxlabs.util.resource.MessageBundle;
import org.foxlabs.util.resource.ResourceHelper;

public class ResourceManager {
    
    private static final String RESOURCE_DIRECTORY =
        ResourceHelper.getResourcePath(ResourceManager.class);
    
    private static final MessageBundle cmdBundle;
    private static final MessageBundle messageBundle;
    
    static {
        cmdBundle = MessageBundle.getInstance(RESOURCE_DIRECTORY + "/cmd");
        messageBundle = MessageBundle.getInstance(RESOURCE_DIRECTORY + "/messages");
    }
    
    private ResourceManager() {}
    
    public static String getMessage(String key) {
        return messageBundle.get(key);
    }
    
    public static String getMessage(String key, Object... arguments) {
        return messageBundle.format(key, arguments);
    }
    
    public static URL getTextTemplateURL() {
        return ResourceHelper.getResourceURL(RESOURCE_DIRECTORY + "/text.template");
    }
    
    public static URL getJavaTemplateURL() {
        return ResourceHelper.getResourceURL(RESOURCE_DIRECTORY + "/java.template");
    }
    
    public static URL getHtmlTemplateURL() {
        return ResourceHelper.getResourceURL(RESOURCE_DIRECTORY + "/html.template");
    }
    
    public static String getGrammarDefaultCssTheme() {
        return ResourceHelper.readTextResource(RESOURCE_DIRECTORY + "/grammar.css", "UTF-8");
    }
    
    public static String getGrammarJsScript() {
        return ResourceHelper.readTextResource(RESOURCE_DIRECTORY + "/grammar.js", "UTF-8");
    }
    
    public static String getGeneratedMessage() {
        return getMessage("copyright.generatedMessage",
                          CommandLine.getProductName(),
                          CommandLine.getProductVersion());
    }
    
    public static String getCommandLineUsage() {
        return cmdBundle.format("usage",
                CommandLine.getProductName(),
                CommandLine.getProductVersion(),
                CommandLine.getProductURL());
    }
    
    // Exceptions
    
    public static IllegalArgumentException newCmdException(String key, Object... arguments) {
        return new IllegalArgumentException(getMessage(key, arguments));
    }
    
}
