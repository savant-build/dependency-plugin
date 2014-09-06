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
import org.objectweb.asm.signature.SignatureReader

/**
 *
 * @author Brian Pontarelli
 */
class ImportMethodVisitor extends MethodVisitor {
  private final Set<String> classes

  ImportMethodVisitor(Set<String> classes) {
    super(Opcodes.ASM5)
    this.classes = classes
  }

  @Override
  void visitParameter(String name, int access) {
    println "MV visitParameter name=${name}"
  }

  @Override
  AnnotationVisitor visitAnnotation(String desc, boolean visible) {
//    println "MV visitAnnotation desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
    return new ImportAnnotationVisitor(classes)
  }

  @Override
  AnnotationVisitor visitAnnotationDefault() {
    return new ImportAnnotationVisitor(classes)
  }

  @Override
  AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
//    println "MV visitTypeAnnotation desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
    return new ImportAnnotationVisitor(classes)
  }

  @Override
  AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
//    println "MV visitParameterAnnotation desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
    return new ImportAnnotationVisitor(classes)
  }

  @Override
  void visitTypeInsn(int opcode, String type) {
//    println "MV visitTypeInsn type=${type}"
    if (type) {
      classes.add(type)
    }
  }

  @Override
  void visitFieldInsn(int opcode, String owner, String name, String desc) {
//    println "MV visitFieldInsn desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
  }

  @Override
  void visitMethodInsn(int opcode, String owner, String name, String desc) {
//    println "MV visitMethodInsn desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
  }

  @Override
  void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
//    println "MV visitMethodInsn desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
  }

  @Override
  void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
//    println "MV visitInvokeDynamicInsn desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
  }

  @Override
  void visitMultiANewArrayInsn(String desc, int dims) {
//    println "MV visitMultiANewArayInsn desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
  }

  @Override
  AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
//    println "MV visitInsnAnnotation desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
    return new ImportAnnotationVisitor(classes)
  }

  @Override
  void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
//    println "MV visitTryCatchBlock type=${type}"
    if (type) {
      classes.add(type)
    }
  }

  @Override
  AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
//    println "MV visitTryCatchAnnotation desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
    return new ImportAnnotationVisitor(classes)
  }

  @Override
  void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
//    println "MV visitLocalVariable name=${name} desc=${ASMTools.getClassName(desc)} signature=${signature}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
    if (signature) {
      new SignatureReader(signature).accept(new ImportSignatureVisitor(classes))
    }
  }

  @Override
  AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
//    println "MV visitLocalVariableAnnotation desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
    return new ImportAnnotationVisitor(classes)
  }
}
