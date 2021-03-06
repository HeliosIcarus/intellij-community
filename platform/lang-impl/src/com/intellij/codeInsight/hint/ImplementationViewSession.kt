// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.hint.PsiImplementationViewSession.getSelfAndImplementations
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.util.Processor

interface ImplementationViewSession {
  val factory: ImplementationViewSessionFactory
  val project: Project

  /**
   * The list of implementations which could be found synchronously. Additional implementations can be obtained by calling
   * [searchImplementationsInBackground].
   */
  val implementationElements: List<ImplementationViewElement>
  val file: PsiFile?

  val element: PsiElement?
  val text: String?
  val editor: Editor?

  fun searchImplementationsInBackground(indicator: ProgressIndicator,
                                        isSearchDeep: Boolean,
                                        includeSelf: Boolean,
                                        processor: Processor<PsiElement>): List<ImplementationViewElement>
  fun elementRequiresIncludeSelf(): Boolean
  fun needUpdateInBackground(): Boolean
}

interface ImplementationViewSessionFactory {
  fun createSession(dataContext: DataContext, project: Project, invokedByShortcut: Boolean): ImplementationViewSession?
  fun createSessionForLookupElement(project: Project, editor: Editor?, file: PsiFile?, lookupItemObject: Any?, isSearchDeep: Boolean): ImplementationViewSession?

  companion object {
    @JvmField val EP_NAME = ExtensionPointName.create<ImplementationViewSessionFactory>("com.intellij.implementationViewSessionFactory")
  }
}

class PsiImplementationSessionViewFactory : ImplementationViewSessionFactory {
  override fun createSession(dataContext: DataContext, project: Project, invokedByShortcut: Boolean): ImplementationViewSession? {
    return PsiImplementationViewSession.create(dataContext, project, invokedByShortcut)
  }

  override fun createSessionForLookupElement(project: Project, editor: Editor?, file: PsiFile?, lookupItemObject: Any?, isSearchDeep: Boolean): ImplementationViewSession? {
    val element = lookupItemObject as? PsiElement ?: DocumentationManager.getInstance(project).getElementFromLookup(editor, file)
    var impls = arrayOf<PsiElement>()
    var text = ""
    if (element != null) {
      // if (element instanceof PsiPackage) return;
      val containingFile = element.containingFile
      if (containingFile == null || !containingFile.viewProvider.isPhysical) return null

      impls = getSelfAndImplementations(editor, element, PsiImplementationViewSession.createImplementationsSearcher(isSearchDeep))
      text = SymbolPresentationUtil.getSymbolPresentableText(element)
    }

    return PsiImplementationViewSession(project, element, impls, text, editor, file)
  }
}
