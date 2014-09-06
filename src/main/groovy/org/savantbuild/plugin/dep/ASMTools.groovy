/*
 * Copyright (c) 2014, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.plugin.dep

import org.objectweb.asm.Type

/**
 *
 * @author Brian Pontarelli
 */
class ASMTools {
  public static String getClassName(String desc) {
    return getClassName(Type.getType(desc))
  }

  public static String getClassName(Type t) {
    switch (t.getSort()) {
      case Type.ARRAY:
        return getClassName(t.getElementType())
      case Type.OBJECT:
        return t.getClassName().replace('.', '/')
    }
    return null
  }
}
