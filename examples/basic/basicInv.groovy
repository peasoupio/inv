inv {
	name "my-webservice"

	require { Server("my-server-id") }

	broadcast { Endpoint } using {
		id "my-webservice-id"
		ready {
			println "my-webservice-id has been broadcast"
		}
	}
}

inv {
	name "my-app"

	require { Endpoint("my-webservice-id") }

	broadcast { App("my-app-id") }
}

inv {
	name "my-server"

	broadcast { Server } using {
		id "my-server-id"
		ready {
			println "my-server-id has been broadcast"
		}
	}
}