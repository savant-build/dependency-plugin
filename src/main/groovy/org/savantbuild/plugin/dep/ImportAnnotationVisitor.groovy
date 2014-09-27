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

import org.objectweb.asm.*

/**
 * ASM AnnotationVisitor that builds a list of classes used within the annotation being visited. This is essentially
 * determining the imports of a Class.
 *
 * @author Brian Pontarelli
 */
class ImportAnnotationVisitor extends AnnotationVisitor {
  private final Set<String> classes

  ImportAnnotationVisitor(Set<String> classes) {
    super(Opcodes.ASM5)
    this.classes = classes
  }

  @Override
  void visit(String name, Object value) {
//    println "AV visit name=${name} value=${value}"
    if (value instanceof Type) {
//      println "Value is ${ASMTools.getClassName(value)}"
      classes.add(ASMTools.getClassName((Type) value))
    }
  }

  @Override
  void visitEnum(String name, String desc, String value) {
//    println "AV visitEnum name=${name} desc=${ASMTools.getClassName(desc)}"
    classes.add(ASMTools.getClassName(desc))
  }

  @Override
  AnnotationVisitor visitAnnotation(String name, String desc) {
//    println "AV visitAnnotation name=${name} desc=${ASMTools.getClassName(desc)}"
    classes.add(ASMTools.getClassName(desc))
    return this
  }

  @Override
  AnnotationVisitor visitArray(String name) {
//    println "AV visitArray name=${name}"
    return this
  }
}
