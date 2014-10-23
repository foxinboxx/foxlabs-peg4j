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

package org.foxlabs.peg4j;

import org.junit.Test;

/**
 * Number of tests that show stack usage.
 * 
 * @author Fox Mulder
 */
public class StackOverflowTest {
    
    private int methodInvocations;
    
    @Test
    public void testStackCapacity() {
        testMethodWithNoArgsAndVars();
        testMethodWithSingleArg();
        testMethodWithSingleVar();
        testMethodWithSingleArgAndVar();
        testMethodWithTenArgs();
        testMethodWithTenVars();
    }
    
    private void testMethodWithNoArgsAndVars() {
        methodInvocations = 0;
        try {
            doMethodWithNoArgsAndVars();
        } catch (StackOverflowError e) {
            System.out.println("Method with no args and vars: " + methodInvocations);
        }
    }
    
    private void doMethodWithNoArgsAndVars() {
        methodInvocations++;
        doMethodWithNoArgsAndVars();
    }
    
    private void testMethodWithSingleArg() {
        methodInvocations = 0;
        try {
            doMethodWithSingleArg(new Object());
        } catch (StackOverflowError e) {
            System.out.println("Method with single arg: " + methodInvocations);
        }
    }
    
    private void doMethodWithSingleArg(Object arg) {
        methodInvocations++;
        doMethodWithSingleArg(arg);
    }
    
    private void testMethodWithSingleVar() {
        methodInvocations = 0;
        try {
            doMethodWithSingleVar();
        } catch (StackOverflowError e) {
            System.out.println("Method with single var: " + methodInvocations);
        }
    }
    
    private void doMethodWithSingleVar() {
        methodInvocations++;
        Object obj = new Object();
        doMethodWithSingleVar();
        obj.hashCode();
    }
    
    private void testMethodWithSingleArgAndVar() {
        methodInvocations = 0;
        try {
            doMethodWithSingleArgAndVar(new Object());
        } catch (StackOverflowError e) {
            System.out.println("Method with single arg and var: " + methodInvocations);
        }
    }
    
    private void doMethodWithSingleArgAndVar(Object arg) {
        methodInvocations++;
        Object obj = new Object();
        doMethodWithSingleArgAndVar(arg);
        obj.hashCode();
    }
    
    private void testMethodWithTenArgs() {
        methodInvocations = 0;
        try {
            doMethodWithTenArgs(new Object(), new Object(), new Object(), new Object(), new Object(),
                                new Object(), new Object(), new Object(), new Object(), new Object());
        } catch (StackOverflowError e) {
            System.out.println("Method with ten args: " + methodInvocations);
        }
    }
    
    private void doMethodWithTenArgs(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4,
                                     Object arg5, Object arg6, Object arg7, Object arg8, Object arg9) {
        methodInvocations++;
        doMethodWithTenArgs(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
    }
    
    private void testMethodWithTenVars() {
        methodInvocations = 0;
        try {
            doMethodWithTenVars();
        } catch (StackOverflowError e) {
            System.out.println("Method with ten vars: " + methodInvocations);
        }
    }
    
    private void doMethodWithTenVars() {
        methodInvocations++;
        Object obj0 = new Object();
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object obj3 = new Object();
        Object obj4 = new Object();
        Object obj5 = new Object();
        Object obj6 = new Object();
        Object obj7 = new Object();
        Object obj8 = new Object();
        Object obj9 = new Object();
        doMethodWithTenVars();
        obj0.hashCode();
        obj1.hashCode();
        obj2.hashCode();
        obj3.hashCode();
        obj4.hashCode();
        obj5.hashCode();
        obj6.hashCode();
        obj7.hashCode();
        obj8.hashCode();
        obj9.hashCode();
    }
    
}
