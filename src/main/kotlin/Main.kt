package org.wurdum

import io.javalin.Javalin
import io.javalin.http.pathParamAsClass
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

fun main() {
    val app = Javalin.create()
    val serial = AtomicInteger()
    val todos = ConcurrentHashMap<Int, Todo>()

    app.get("/todos") { ctx ->
        ctx.json(
            todos
                .asSequence()
                .sortedBy { it.value.id }
                .map { it.value }
                .toList()
        )
    }

    app.post("/todos") { ctx ->
        val todo = ctx.bodyValidator(AddTodoRequest::class.java)
            .check({ it.text.isNotBlank() }, "Text cannot be blank")
            .get()

        val id = serial.incrementAndGet()
        todos[id] = Todo(id, todo.text, false)

        ctx.status(201)
    }

    app.put("/todos/{id}") { ctx ->
        val id = ctx.pathParamAsClass<Int>("id")
            .check({ it > 0 }, "ID must be greater than 0")
            .get()

        val todo = todos[id]
        if (todo == null) {
            ctx.status(404)
            return@put
        }

        val updatedTodo = ctx.bodyValidator(UpdateTodoRequest::class.java)
            .check({ it.text.isNotBlank() }, "Text cannot be blank")
            .get()

        todos[id] = todo.copy(text = updatedTodo.text)
    }

    app.post("/todos/{id}:toggle") { ctx ->
        val id = ctx.pathParamAsClass<Int>("id")
            .check({ it > 0 }, "ID must be greater than 0")
            .get()

        val todo = todos[id]
        if (todo == null) {
            ctx.status(404)
            return@post
        }

        todos[id] = todo.copy(done = !todo.done)
    }

    app.start(7070)
}

data class AddTodoRequest(val text: String)
data class UpdateTodoRequest(val text: String)
data class Todo(val id: Int, val text: String, val done: Boolean)