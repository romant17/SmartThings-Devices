metadata {
	// Automatically generated. Make future change here.
	definition (name: "My Z-Wave Metering Switch - Aeon", namespace: "jscgs350", author: "SmartThings") {
		capability "Energy Meter"
		capability "Actuator"
		capability "Switch"
		capability "Power Meter"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        capability "Configuration"
        capability "Sensor"

        attribute "power", "string"
        attribute "powerDisp", "string"
        attribute "powerOne", "string"
        attribute "powerTwo", "string"
        
        command "reset"
        command "configure"
        
		fingerprint inClusters: "0x25,0x32"
	}


	// tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}

// Watts row

        valueTile("powerDisp", "device.powerDisp", inactiveLabel: false, decoration: "flat") {
            state ("default", label:'${currentValue}', backgroundColor:"#81D8D0")
        }
        
        valueTile("powerOne", "device.powerOne", inactiveLabel: false, decoration: "flat") {
            state("default", label:'${currentValue}', backgroundColor:"#c3cb71")
        }
        
        valueTile("powerTwo", "device.powerTwo", inactiveLabel: false, decoration: "flat") {
            state("default", label:'${currentValue}', backgroundColor:"#e0301e")
        }
//

		valueTile("energy", "device.energy", decoration: "flat") {
			state "default", label:'${currentValue} kWh'
		}
		standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat") {
			state "default", label:'Reset', action:"reset"
		}
		standardTile("configure", "device.power", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

//        main (["powerDisp","energyDisp"])
//        details(["powerOne","powerDisp","powerTwo","reset","refresh", "configure"])
//    	}

		main "switch"
		details(["switch","energy","refresh","powerOne","powerDisp","powerTwo","reset","configure"])
	}
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x20: 1, 0x32: 1])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
    log.debug "zwaveEvent received ${cmd}"
    def dispValue
    def newValue
    def timeString = new Date().format("h:mm a", location.timeZone)
	if (cmd.scale == 0) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kWh"]
	} else if (cmd.scale == 1) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kVAh"]
	}
	else {
            newValue = Math.round( cmd.scaledMeterValue )       // really not worth the hassle to show decimals for Watts
            if (newValue != state.powerValue) {
                dispValue = newValue+"\nWatts"
                sendEvent(name: "powerDisp", value: dispValue as String, unit: "")
                
                if (newValue < state.powerLow) {
                    dispValue = "Low\n"+newValue+" W\n"+timeString
                    sendEvent(name: "powerOne", value: dispValue as String, unit: "")
                    state.powerLow = newValue
                }
                if (newValue > state.powerHigh) {
                    dispValue = "High\n"+newValue+" W\n"+timeString
                    sendEvent(name: "powerTwo", value: dispValue as String, unit: "")
                    state.powerHigh = newValue
                }
                state.powerValue = newValue
                [name: "power", value: newValue, unit: "W"]
            }
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
	[
		name: "switch", value: cmd.value ? "on" : "off", type: "physical"
	]
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
	[
		name: "switch", value: cmd.value ? "on" : "off", type: "digital"
	]
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def on() {
	delayBetween([
			zwave.basicV1.basicSet(value: 0xFF).format(),
			zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def off() {
	delayBetween([
			zwave.basicV1.basicSet(value: 0x00).format(),
			zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def poll() {
    refresh()
}

def refresh() {
    log.debug "${device.name} refresh"
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 2).format()
	])
}

def reset() {
    log.debug "${device.name} reset"

    state.powerHigh = 0
    state.powerLow = 99999

    sendEvent(name: "powerOne", value: "", unit: "")    
    sendEvent(name: "powerDisp", value: "", unit: "")    
    sendEvent(name: "powerTwo", value: "", unit: "")    

    def cmd = delayBetween( [
        zwave.meterV2.meterReset().format(),
        zwave.meterV2.meterGet(scale: 0).format()
    ])
    
    cmd
}

def configure() {
    log.debug "${device.name} configure"
	delayBetween([
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 4).format(),   // combined power in watts
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 30).format(), // every 30 seconds
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 8).format(),   // combined energy in kWh
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 150).format(), // every 2.5 minutes
		zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),    // no third report
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 300).format() // every 5 min
	])
}
