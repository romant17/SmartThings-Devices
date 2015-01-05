/**
 *  SmartSense Virtual Momentary Contact Switch, Better Momentary for the MIMOlite
 *
 *
 *  Author: SmartThings, jscgs350
 *  Date: 2013-03-07,2014-02-03, 2014-03-07, 2015-01-04
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "My MIMOlite - Garage Car Door v2", namespace: "jscgs350", author: "jscgs350") {
		capability "Refresh"
		capability "Contact Sensor"
		capability "Momentary"
		capability "Polling"
		capability "Switch"
        attribute "power", "string"

	}

	// tile definitions
	tiles {
        standardTile("contact", "device.contact", width: 2, height: 2, inactiveLabel: false) {
            state "open", label: 'Open', action: "momentary.push", icon: "st.doors.garage.garage-open", backgroundColor: "#ffa81e"
            state "closed", label: 'Closed', action: "momentary.push", icon: "st.doors.garage.garage-closed", backgroundColor: "#79b821"
        }
        standardTile("power", "device.power", inactiveLabel: false) {
        	state "dead", label: 'OFF', icon:"st.switches.switch.off", backgroundColor: "#ff0000"
        	state "alive", label: 'ON', icon:"st.switches.switch.on", backgroundColor: "#79b821"
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
        main (["contact"])
        details(["contact", "power", "refresh", "configure"])
    }
}

def parse(String description) {
    def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x84: 1, 0x30: 1, 0x70: 1, 0x31: 3, 0x71: 1])

    if (cmd.CMD == "7105") {				//Mimo sent a power report lost power
        sendEvent(name: "power", value: "dead")
    } else {
    	sendEvent(name: "power", value: "alive")
    }

	if (cmd) {
        result = createEvent(zwaveEvent(cmd))
    }
//    log.debug "Parse returned ${result?.descriptionText}"
    return result
}

def sensorValueEvent(Short value) {
    if (value) {
    	log.debug "$device.displayName is now open"
		createEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open")
    } else {
        log.debug "$device.displayName is now closed"
        createEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport cmd)
{
	log.debug "zwaveEvent AlarmReport: '${cmd}'"

    switch (cmd.alarmType) {
        case 8:
            def map = [ name: "power", isStateChange:true]
            if (cmd.alarmLevel){
                map.value="dead"
                map.descriptionText = "${device.displayName} lost power"
            }
            else {
                map.value="alive"
                map.descriptionText = "${device.displayName} has power"
            }
            createEvent(map)
        break;
		default:
        	[:]
        break;
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def push() {
	log.debug "Executing ACTUATE command for garage car door per user request"
	def cmds = [
		zwave.basicV1.basicSet(value: 0xFF).format(),
	]
}

def poll() {
	log.debug "Executing Poll for garage car door"
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],100)
}

def refresh() {
	log.debug "Executing Refresh for garage car door per user request"
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],100)
}

def configure() {
	log.debug "Executing Configure for garage car door per user request"
	def cmd = delayBetween([
//        zwave.configurationV1.configurationSet(parameterNumber: 11, size: 1, configurationValue: [0]).format(), // momentary relay disable=0 (default)
        zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:zwaveHubNodeId).format(),	//subscribe to power alarm
	],100)
}