package com.halkow.emote.api

import groovy.util.logging.Slf4j

import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.transform.stream.StreamSource

import com.halkow.emote.schema.EmotivaPing
import com.halkow.emote.schema.EmotivaTransponder

@Slf4j
class EmoteClient {
  private final static int BROADCAST_PORT = 7000
  private final static int DISCOVERY_PORT = 7001


  private UDPServer discoveryServer
  private def notifyServers = [:]
  private def controlServers = [:]


  private Marshaller marshaller
  private Unmarshaller unmarshaller

  Closure discovered


  EmoteClient() {
    this.discoveryServer = new UDPServer(DISCOVERY_PORT)

    this.discoveryServer.messageReceived = { discAddress, discMessage ->
      Object object = unmarshaller.unmarshal(
        new StreamSource(new StringReader(discMessage)))

      if (object instanceof EmotivaTransponder) {

        def device = new EmoteDevice(
          client: this,
          name: object.name,
          model: object.model,
          socketAddress: discAddress,
          version: object.control.version,
          controlPort: object.control.controlPort,
          notifyPort: object.control.notifyPort,
          infoPort: object.control.infoPort,
          setupPortTCP: object.control.setupPortTCP
        )

        UDPServer notifyServer = this.notifyServers[device.notifyPort]
        if (notifyServer == null) {
          notifyServer = new UDPServer(device.notifyPort)
          this.notifyServers[device.notifyPort] = notifyServer
          notifyServer.messageReceived = { notifyAddress, notifyMessage ->
            println "notify message from " +
              "${notifyAddress}:\n${notifyMessage}"
          }
          notifyServer.start()
        }

        UDPServer controlServer = this.controlServers[device.controlPort]
        if (controlServer == null) {
          controlServer = new UDPServer(device.controlPort)
          this.controlServers[device.controlPort] = controlServer
          controlServer.messageReceived = { controlAddress, controlMessage ->
            println "control message from " +
              "${controlAddress}:\n${controlMessage}"
          }
          controlServer.start()
        }

        if (this.discovered != null) {
          this.discovered(device)
        }
      }
    }
    this.discoveryServer.start()


    JAXBContext jaxbContext = JAXBContext.newInstance("com.halkow.emote.schema")

    // JAXB unmarshaller
    this.unmarshaller = jaxbContext.createUnmarshaller()

    // JAXB marshaller
    this.marshaller = jaxbContext.createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8")
  }


  private String marshal(Object o) {
    StringWriter stringWriter = new StringWriter()
    marshaller.marshal(o, stringWriter)
    stringWriter.toString()
  }

  def send(Object o, InetAddress address, int port) {
    String message = this.marshal(o)
    byte[] sendData = message.bytes
    println "sending message to ${address}:${port}\n${message}"


    DatagramPacket sendPacket = new DatagramPacket(
        sendData,
        sendData.length,
        address,
        port)


    DatagramSocket socket = new DatagramSocket()
    socket.send(sendPacket)
  }


  def discover() {
    EmotivaPing ping = new EmotivaPing()
    StringWriter stringWriter = new StringWriter()
    marshaller.marshal(ping, stringWriter)
    byte[] sendData = stringWriter.toString().bytes
    DatagramPacket sendPacket = new DatagramPacket(
        sendData,
        sendData.length,
        InetAddress.getByName("255.255.255.255"),
        BROADCAST_PORT)

    DatagramSocket socket = new DatagramSocket()
    socket.broadcast = true
    socket.send(sendPacket)

    /*
    if (this.discovered != null) {
      this.discovered(new EmoteDevice(name: "XMC-1"))
    }
    */
  }


  EmoteClient start() {
    this.discoveryServer.start()
    this
  }

  EmoteClient stop() {
    this.discoveryServer.stop()
    this.notifyServers.each { port, notifyServer ->
      notifyServer.stop()
    }
    this.controlServers.each { port, controlServer ->
      controlServer.stop()
    }
    this
  }
}
