package systems.moellers.undertow.handler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.PathTemplateMatch
import systems.moellers.undertow.error.BadRequest
import systems.moellers.undertow.error.NotFound
import systems.moellers.undertow.model.Pet

class Router : RoutingHandler() {
    init {
        // Define Handlers
        val mapper = jacksonObjectMapper()

        val pets = hashMapOf(
            "corny" to Pet("corny", 24),
            "markus" to Pet("markus", 25)
        )

        post("/pet", BlockingHandler { exchange ->
            val pet: Pet = mapper.readValue(exchange.inputStream)
            pets[pet.name] = pet

            val json = mapper.writeValueAsString(pet)
            exchange.responseSender.send(json)
        })

        get("/pet/{name}", { exchange ->
            val pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY)
            val name = pathMatch.parameters["name"]
            name ?: throw BadRequest("No pet name provided")

            val pet = pets[name]
            pet ?: throw NotFound("Could not find pet")

            exchange.responseSender.send(mapper.writeValueAsString(pet))
        })

        put("/pet/{name}", BlockingHandler { exchange ->
            val pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY)
            val name = pathMatch.parameters["name"]
            name ?: throw BadRequest("No pet name provided")

            val pet: Pet = mapper.readValue(exchange.inputStream)

            pets[name] = pet
            exchange.responseSender.send(mapper.writeValueAsString(pet))
        })

        delete("/pet/{name}", { exchange ->
            val pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY)
            val name = pathMatch.parameters["name"]
            name ?: throw BadRequest("No pet name provided")

            val pet = pets[name]
            pet ?: throw NotFound("Could not find pet")

            pets.remove(name)
            exchange.responseSender.send(mapper.writeValueAsString(pet))
        })

        post("/pet/{name}/increaseAge", { exchange ->
            val pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY)
            val name = pathMatch.parameters["name"]
            name ?: throw BadRequest("No pet name provided")

            val pet = pets[name]
            pet ?: throw NotFound("Could not find pet")

            pet.age += 1
            exchange.responseSender.send(mapper.writeValueAsString(pet))
        })
    }
}
