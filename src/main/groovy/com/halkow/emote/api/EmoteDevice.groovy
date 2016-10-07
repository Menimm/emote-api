package com.halkow.emote.api

import com.halkow.emote.schema.EmotivaControl
import com.halkow.emote.schema.EmotivaSubscription
import com.halkow.emote.schema.YesNo

class EmoteDevice {
  EmoteClient client
  String name
  String model
  String version
  SocketAddress socketAddress
  int controlPort
  int notifyPort
  int infoPort
  int setupPortTCP


  EmoteDevice powerOn() {

    EmotivaControl control = new EmotivaControl()
    //control.power_on.value = 0
    //control.power_on.ack = "yes"

    def powerOn = new EmotivaControl.PowerOn(value: 0, ack: YesNo.YES)
    control.powerOn.push(powerOn)

    this.client.send(control, socketAddress.address, this.controlPort)
    this
  }

  //
  def setVolumeChanged(Closure c) {
    if (c != null) {
      // subscribe
      def subscribe = new EmotivaSubscription()
      subscribe.children.push(new EmotivaSubscription.Volume())
      this.client.send(subscribe, socketAddress.address, this.controlPort)
    } else {
      // unsubscribe
    }
  }

  /*
  static boolean ON = true
  static boolean OFF = false


  Closure subscribed
  Closure powerUpdated
  Closure sourceUpdated
  Closure dimUpdated

  EmoteDevice power(boolean on) {
    if (this.powerUpdated != null) this.powerUpdated(on)
    this
  }

  EmoteDevice source(String source) {
    if (this.sourceUpdated != null) this.sourceUpdated(source)
    this
  }

  EmoteDevice dim(int dim) {
    if (this.dimUpdated != null) this.dimUpdated(dim)
    this
  }

  void updateSubscriptions() {
  }
  */
}
