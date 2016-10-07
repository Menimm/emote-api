package com.halkow.emote.api

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.Selector
import java.nio.channels.SelectionKey
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.util.concurrent.Future
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class UDPServer {

  int port

  UDPServer(int port) {
    this.port = port
  }

  Closure messageReceived

  private static int BUFFER_SIZE = 1024

  private Thread thread

  void stop() {
    thread.interrupt()
  }

  void start() {

    Charset charset = Charset.forName("UTF-8")
    CharsetDecoder decoder = charset.newDecoder()

    DatagramChannel serverChannel = DatagramChannel.open()
    serverChannel.socket().bind(new InetSocketAddress(this.port))
    serverChannel.configureBlocking(false)

    Selector selector = Selector.open()
    serverChannel.register(selector, SelectionKey.OP_READ)


    thread = Thread.start {
      while (!Thread.currentThread().isInterrupted()) {
        selector.select()

        Set<SelectionKey> keys = selector.selectedKeys()
        keys.each { SelectionKey key ->
          keys.remove(key)
          if (key.isValid()) {
            if (key.isReadable()) {
              DatagramChannel channel = (DatagramChannel)key.channel()

              ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
              SocketAddress clientAddress = channel.receive(readBuffer)
              if (clientAddress != null) {
                readBuffer.flip()

                String message = decoder.decode(readBuffer)
                if (this.messageReceived != null) {
                  this.messageReceived(clientAddress, message)
                }
              }
            }
          }
        }
      }
    }
  }
}
