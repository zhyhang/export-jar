package org.yanhuang.plugins.intellij.exportjar.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.changes.CommitResultHandler
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.vcs.commit.SingleChangeListCommitWorkflow

/**
 * helper for java invoking: (1)use var-args constructor in code wrote by Kotlin; (2) further compatible with version 2020.2 in class SingleChangeListCommitWorkflow
 */
class WorkflowHelper() {
    fun createWorkflow(
        project: Project,
        affectedVcses: Set<AbstractVcs>,
        initiallyIncluded: Collection<Any>,
        initialChangeList: LocalChangeList
    ): SingleChangeListCommitWorkflow {
        try {
            return reflectAfterVer2022(project, affectedVcses, initiallyIncluded, initialChangeList)
        }catch (e:Exception){
            return reflectBeforeVer2022(project, affectedVcses, initiallyIncluded, initialChangeList)
        }
    }

    private fun reflectAfterVer2022(
        project: Project,
        affectedVcses: Set<AbstractVcs>,
        initiallyIncluded: Collection<Any>,
        initialChangeList: LocalChangeList
    ): SingleChangeListCommitWorkflow {
        val constructor = SingleChangeListCommitWorkflow::class.java.getConstructor(
            Project::class.java,
            Set::class.java,
            Collection::class.java,
            LocalChangeList::class.java,
            List::class.java,
            Boolean::class.java,
            String::class.java,
            CommitResultHandler::class.java
        )
        return constructor.newInstance(
            project, affectedVcses, initiallyIncluded, initialChangeList,
            emptyList<Any>(), true, null, null
        )
    }

    private fun reflectBeforeVer2022(
        project: Project,
        affectedVcses: Set<AbstractVcs>,
        initiallyIncluded: Collection<Any>,
        initialChangeList: LocalChangeList
    ): SingleChangeListCommitWorkflow {
        val constructor = SingleChangeListCommitWorkflow::class.java.getConstructor(
            Project::class.java,
            Set::class.java,
            Collection::class.java,
            LocalChangeList::class.java,
            List::class.java,
            Boolean::class.java,
            Boolean::class.java,
            String::class.java,
            CommitResultHandler::class.java
        )
        return constructor.newInstance(
            project, affectedVcses, initiallyIncluded, initialChangeList,
            emptyList<Any>(), true, true, null, null
        )
    }
}