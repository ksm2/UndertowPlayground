package systems.moellers.undertow.handler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument.AFTER as after
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.PathTemplateMatch
import org.litote.kmongo.*
import org.litote.kmongo.MongoOperator.*
import systems.moellers.undertow.error.BadRequest
import systems.moellers.undertow.error.NotFound
import systems.moellers.undertow.model.Pet

fun HttpServerExchange.param(name: String): String {
    val pathMatch = getAttachment(PathTemplateMatch.ATTACHMENT_KEY)
    return pathMatch.parameters[name] ?: throw BadRequest("Missing parameter: $name")
}

fun HttpServerExchange.ok(value: Any) {
    responseSender.send(value.json)
}

fun java.io.InputStream.convertStreamToString(): String {
    val s = java.util.Scanner(this).useDelimiter("\\A")
    return if (s.hasNext()) s.next() else ""
}

class Router : RoutingHandler() {
    init {
        // JSON Mapper
        val mapper = jacksonObjectMapper()

        // Mongo DB
        val client = KMongo.createClient("mongo")
        val database = client.getDatabase("petstore")
        val pets = database.getCollection<Pet>()

        get("/pet", { exchange ->
            exchange.ok(pets.find())
        })

        post("/pet", BlockingHandler { exchange ->
            val pet: Pet = mapper.readValue(exchange.inputStream)
            pets.insertOne(pet)

            exchange.ok(pet)
        })

        get("/pet/{name}", { exchange ->
            val name = exchange.param("name")

            val pet = pets.findOne("{name: ${name.json}}")
            pet ?: throw NotFound("Could not find pet")

            exchange.ok(pet)
        })

        put("/pet/{name}", BlockingHandler { exchange ->
            val name = exchange.param("name")

            val update = exchange.inputStream.convertStreamToString()
            val pet: Pet = pets.findOneAndUpdate("{name: ${name.json}}", "{$set: $update}", FindOneAndUpdateOptions().returnDocument(after))
            pet ?: throw NotFound("Could not find pet")

            exchange.ok(pet)
        })

        delete("/pet/{name}", { exchange ->
            val name = exchange.param("name")

            val pet: Pet? = pets.findOneAndDelete("{name: ${name.json}}")
            pet ?: throw NotFound("Could not find pet")

            exchange.ok(pet)
        })

        post("/pet/{name}/increaseAge", { exchange ->
            val name = exchange.param("name")

            val pet = pets.findOneAndUpdate("{name: ${name.json}}", "{$inc: {age: 1}}", FindOneAndUpdateOptions().returnDocument(after))
            pet ?: throw NotFound("Could not find pet")

            exchange.ok(pet)
        })
    }
}
