package me.naotiki.sucideaplugin.listeners

import Event
import ServerProtocol
import SocketService
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runToolbar.getDisplayName
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.psi.PsiFile


class MyListener(val project: Project) : ExecutionListener {
    val socketService=project.getService(SocketService::class.java)
    //ビルド開始
    override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {

        socketService.sendData(
            ServerProtocol.SendEvent(
                Event.StartBuild(executorId)
            )
        )

    }

    override fun processNotStarted(executorId: String, env: ExecutionEnvironment, cause: Throwable?) {
        socketService.sendData(
            ServerProtocol.SendEvent(
                Event.FailedBuild(executorId)
            )
        )
    }

    //ビルド終了
    override fun processTerminated(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
        exitCode: Int
    ) {

        socketService.sendData(
            ServerProtocol.SendEvent(
                if (exitCode==0)
                    Event.SuccessBuild(executorId)
                else Event.FailedBuild(executorId)
            )
        )
    }

}

class SUCTypedHandler : TypedHandlerDelegate() {
    //IJの補完入力は愛がこもっていないので反応しない。
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        println("Typed:$c")
        project.getService(SocketService::class.java).sendData(ServerProtocol.SendEvent(Event.Typed(c.toString())))
        return super.charTyped(c, project, editor, file)
    }

}

class ProjectStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        val started = project
            .getService(SocketService::class.java)
        started.startServer()
        started.sendData(ServerProtocol.Hello)
        started.sendData(ServerProtocol.SendEvent(
            Event.OpenProject(project.name)
        ))
        println("Server Started : $started")
    }

}
