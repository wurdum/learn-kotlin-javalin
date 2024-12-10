package org.wurdum

import io.javalin.Javalin
import io.javalin.http.pathParamAsClass
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

val appModule = module {
    single { TodoRepository() }
}

fun main() {
    startKoin { modules(appModule) }

    Application()
        .setup()
        .start(7070)
}

class Application : KoinComponent {
    private val repository: TodoRepository by inject()

    fun setup(): Javalin {
        val app = Javalin.create()

        app.get("/todos") { ctx ->
            ctx.json(repository.list())
        }

        app.post("/todos") { ctx ->
            val todo = ctx.bodyValidator(AddTodoRequest::class.java)
                .check({ it.text.isNotBlank() }, "Text cannot be blank")
                .get()

            repository.add(todo.text)

            ctx.status(201)
        }

        app.put("/todos/{id}") { ctx ->
            val id = ctx.pathParamAsClass<Int>("id")
                .check({ it > 0 }, "ID must be greater than 0")
                .get()

            val updatedTodo = ctx.bodyValidator(UpdateTodoRequest::class.java)
                .check({ it.text.isNotBlank() }, "Text cannot be blank")
                .get()

            val updated = repository.update(id, updatedTodo.text)
            if (!updated) {
                ctx.status(404)
            }
        }

        app.post("/todos/{id}:toggle") { ctx ->
            val id = ctx.pathParamAsClass<Int>("id")
                .check({ it > 0 }, "ID must be greater than 0")
                .get()

            val toggled = repository.toggle(id)
            if (!toggled) {
                ctx.status(404)
            }
        }

        return app
    }
}

data class AddTodoRequest(val text: String)
data class UpdateTodoRequest(val text: String)
data class Todo(val id: Int, val text: String, val done: Boolean)

class TodoRepository {

    companion object {
        private val serial = AtomicInteger()
        private val todos = ConcurrentHashMap<Int, Todo>()
    }

    fun list(): List<Todo> {
        return todos
            .asSequence()
            .sortedBy { it.value.id }
            .map { it.value }
            .toList()
    }

    fun add(text: String) {
        val id = serial.incrementAndGet()
        todos[id] = Todo(id, text, false)
    }

    fun update(id: Int, text: String): Boolean {
        val existingTodo = todos[id] ?: return false
        todos[id] = existingTodo.copy(text = text)
        return true
    }

    fun toggle(id: Int): Boolean {
        val existingTodo = todos[id] ?: return false
        todos[id] = existingTodo.copy(done = !existingTodo.done)
        return true
    }
}