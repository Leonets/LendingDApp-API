package xrd.zerocollateral.servers

import org.http4k.core.*
import org.http4k.lens.string
import xrd.zerocollateral.Config

object StaticConfigHandler:  HttpHandler {

    private val bodyLens = Body.string(contentType = ContentType.APPLICATION_JSON).toLens()


    private val staticConfigDoc = "{" +
            "\"dbName\": \""+Config.dbName+"\"" +
            "}"

    override fun invoke(req: Request): Response = Response(Status.OK).with(bodyLens of staticConfigDoc)
}