inv {
	name "my-webservice"

	require inv.Server("my-server-id")

	broadcast inv.Endpoint using {
		id "my-webservice-id"
		ready {
			println "my-webservice-id has been broadcast"
		}
	}
}

inv {
	name "my-app"

	require inv.Endpoint("my-webservice-id")

	broadcast inv.App("my-app-id")
}

inv {
	name "my-server"

	broadcast inv.Server using {
		id "my-server-id"
		ready {
			println "my-server-id has been broadcast"
		}
	}
}