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
class ImportClassVisitor extends ClassVisitor {
  public Set<String> classes = new HashSet<>()

  ImportClassVisitor() {
    super(Opcodes.ASM5)
  }

  @Override
  void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
//    println "CV visit name=${name} signature=${signature} superName=${superName} interfaces=${interfaces}"
    if (superName) {
      classes.add(superName)
    }
    if (interfaces) {
      classes.addAll(interfaces)
    }
    if (signature) {
      new SignatureReader(signature).accept(new ImportSignatureVisitor(classes))
    }
  }

  @Override
  AnnotationVisitor visitAnnotation(String desc, boolean visible) {
//    println "CV visitAnnotation desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
    return new ImportAnnotationVisitor(classes)
  }

  @Override
  AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
//    println "CV visitTypeAnnotation desc=${ASMTools.getClassName(desc)}"
    if (desc) {
      classes.add(ASMTools.getClassName(desc))
    }
    return new ImportAnnotationVisitor(classes)
  }

  @Override
  FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
//    println "CV visitField name=${name} desc=${ASMTools.getClassName(desc)} signature=${signature}"
    if (desc) {
      String className = ASMTools.getClassName(desc)
      if (className) {
        classes.add(className)
      }
    }
    if (signature) {
      new SignatureReader(signature).accept(new ImportSignatureVisitor(classes))
    }
    return new ImportFieldVisitor(classes)
  }

  @Override
  MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//    println "CV visitMethod name=${name} desc=${ASMTools.getClassName(desc)} signature=${signature} exceptions=${exceptions}"
    String returnClassName = ASMTools.getClassName(Type.getReturnType(desc))
    if (returnClassName) {
      classes.add(returnClassName)
    }

    Type[] types = Type.getArgumentTypes(desc);
    for(int i = 0; i < types.length; i++) {
      String argumentClassName = ASMTools.getClassName(types[i])
      if (argumentClassName) {
        classes.add(argumentClassName)
      }
    }

    if (exceptions) {
      classes.addAll(exceptions)
    }
    if (signature) {
      new SignatureReader(signature).accept(new ImportSignatureVisitor(classes))
    }

    return new ImportMethodVisitor(classes)
  }
}
