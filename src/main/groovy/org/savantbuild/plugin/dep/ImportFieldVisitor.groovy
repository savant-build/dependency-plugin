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

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.TypePath

/**
 * ASM FieldVisitor that builds a list of classes used by the field being visited. This is essentially determining
 * the imports of a Class.
 *
 * @author Brian Pontarelli
 */
class ImportFieldVisitor extends FieldVisitor {
  private final Set<String> classes

  ImportFieldVisitor(Set<String> classes) {
    super(Opcodes.ASM5)
    this.classes = classes
  }

  @Override
  AnnotationVisitor visitAnnotation(String desc, boolean visible) {
//    println "FV visitAnnotation desc=${ASMTools.getClassName(desc)}"
    classes.add(ASMTools.getClassName(desc))
    return new ImportAnnotationVisitor(classes)
  }

  @Override
  AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
//    println "FV visitTypeAnnotation ${ASMTools.getClassName(desc)}"
    classes.add(ASMTools.getClassName(desc))
    return new ImportAnnotationVisitor(classes)
  }
}
