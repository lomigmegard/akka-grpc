package akka.http.grpc

import akka.grpc.gen.CodeGenerator
import akka.http.grpc.javadsl.JavaServerCodeGenerator
import akka.http.grpc.scaladsl.ScalaServerCodeGenerator
import com.google.protobuf.compiler.PluginProtos
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import protocbridge.Artifact

import scala.collection.JavaConverters._
/**
 * Generate both Java and Scala server-side code, mainly for testing.
 */
object CompositeCodeGenerator extends CodeGenerator {
  override val name = "akka-composite"

  override def run(request: PluginProtos.CodeGeneratorRequest): PluginProtos.CodeGeneratorResponse = {
    val javaResult = JavaServerCodeGenerator.run(request)
    val scalaResult = ScalaServerCodeGenerator.run(request)
    println(javaResult.getFileList.asScala.map(_.getName))
    println(scalaResult.getFileList.asScala.map(_.getName))
    CodeGeneratorResponse.newBuilder()
      .addAllFile(javaResult.getFileList)
      .addAllFile(scalaResult.getFileList)
      .build()
  }

  override def suggestedDependencies: Seq[Artifact] =
    JavaServerCodeGenerator.suggestedDependencies ++ ScalaServerCodeGenerator.suggestedDependencies
}