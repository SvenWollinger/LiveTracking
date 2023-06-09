package de.swm.livetracking

import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.core.MessageProducer
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter
import org.springframework.messaging.MessageHandler

@SpringBootApplication
@IntegrationComponentScan
class FrontendApplication {
    @Bean
    fun mqttClientFactory() = DefaultMqttPahoClientFactory().apply {
        connectionOptions = MqttConnectOptions().apply {
            serverURIs = arrayOf(ApplicationSettings.url)
        }
    }

    @Bean
    fun mqttInboundChannel() = DirectChannel()

    @Bean
    fun inbound(): MessageProducer {
        return MqttPahoMessageDrivenChannelAdapter(ApplicationSettings.url, "bus-changeme", ApplicationSettings.topic + "/#").apply {
            setCompletionTimeout(5000)
            setConverter(DefaultPahoMessageConverter())
            setQos(1)
            outputChannel = mqttInboundChannel()
        }
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    fun handler() = MessageHandler { message ->
        val topicRaw = ApplicationSettings.topic.replace("#", "")
        var vehicleID = message.headers["mqtt_receivedTopic"].toString().replace(topicRaw, "")
        if(vehicleID.startsWith("/")) vehicleID = vehicleID.replaceFirst("/", "")
        println("$vehicleID: ${message.payload}")
    }
}