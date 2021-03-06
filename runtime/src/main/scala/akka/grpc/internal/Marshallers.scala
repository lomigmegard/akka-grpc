/*
 * Copyright (C) 2018-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.grpc.internal

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, InputStream }

import io.grpc.KnownLength
import akka.annotation.InternalStableApi
import akka.grpc.ProtobufSerializer

/**
 * INTERNAL API
 */
@InternalStableApi
abstract class BaseMarshaller[T](val protobufSerializer: ProtobufSerializer[T])
    extends io.grpc.MethodDescriptor.Marshaller[T]
    with WithProtobufSerializer[T] {
  override def parse(stream: InputStream): T = {
    val buffer =
      new Array[Byte](stream match {
        case k: KnownLength => math.max(0, k.available()) // No need to oversize this if we already know the size
        case _              => 32 * 1024
      })

    // Blocking calls underneath...
    // we can't avoid it for the moment because we are relying on the Netty's Channel API
    val initialBytes = stream.read(buffer, 0, buffer.length)
    val nextByte = if (initialBytes < 0) -1 else stream.read() // Test for EOF
    val bytes =
      if (nextByte == -1) {
        if (initialBytes < 1) akka.util.ByteString.empty // EOF immediately
        else {
          // WARNING: buffer is retained in full below,
          // which could be problematic if ProtobufSerializer.deserialize keeps a reference to the ByteString
          akka.util.ByteString.fromArrayUnsafe(buffer, 0, initialBytes)
        }
      } else {
        val baos = new ByteArrayOutputStream(buffer.length * 2) // To avoid immediate resize
        baos.write(buffer, 0, initialBytes)
        baos.write(nextByte)

        var bytesRead = stream.read(buffer)
        while (bytesRead >= 0) {
          baos.write(buffer, 0, bytesRead)
          bytesRead = stream.read(buffer)
        }

        akka.util.ByteString.fromArrayUnsafe(baos.toByteArray)
      }

    protobufSerializer.deserialize(bytes)
  }
}

/**
 * INTERNAL API
 */
@InternalStableApi
final class Marshaller[T <: scalapb.GeneratedMessage](protobufSerializer: ProtobufSerializer[T])
    extends BaseMarshaller[T](protobufSerializer) {
  override def parse(stream: InputStream): T = super.parse(stream)
  override def stream(value: T): InputStream =
    new ByteArrayInputStream(value.toByteArray) with KnownLength
}

/**
 * INTERNAL API
 */
@InternalStableApi
class ProtoMarshaller[T <: com.google.protobuf.Message](protobufSerializer: ProtobufSerializer[T])
    extends BaseMarshaller[T](protobufSerializer) {
  override def parse(stream: InputStream): T = super.parse(stream)
  override def stream(value: T): InputStream =
    new ByteArrayInputStream(value.toByteArray) with KnownLength
}
