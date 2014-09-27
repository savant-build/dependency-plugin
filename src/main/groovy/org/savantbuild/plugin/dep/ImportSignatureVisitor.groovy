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

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor

/**
 * ASM SignatureVisitor that builds a list of classes used within a generic signature of a method, field, or class.
 * This is essentially determining the imports of a Class.
 *
 * @author Brian Pontarelli
 */
class ImportSignatureVisitor extends SignatureVisitor {
  private final Set<String> classes

  ImportSignatureVisitor(Set<String> classes) {
    super(Opcodes.ASM5)
    this.classes = classes
  }

  @Override
  void visitClassType(String name) {
//    println "SV visitClassType name=${name}"
    classes.add(name)
  }

  @Override
  void visitInnerClassType(String name) {
//    println "SV visitInnerClassType name=${name}"
    classes.add(name)
  }

  @Override
  SignatureVisitor visitClassBound() {
    return this
  }

  @Override
  SignatureVisitor visitInterfaceBound() {
    return this
  }

  @Override
  SignatureVisitor visitSuperclass() {
    return this
  }

  @Override
  SignatureVisitor visitInterface() {
    return this
  }

  @Override
  SignatureVisitor visitParameterType() {
    return this
  }

  @Override
  SignatureVisitor visitReturnType() {
    return this
  }

  @Override
  SignatureVisitor visitExceptionType() {
    return this
  }

  @Override
  SignatureVisitor visitArrayType() {
    return this
  }

  @Override
  SignatureVisitor visitTypeArgument(char wildcard) {
    return this
  }
}
