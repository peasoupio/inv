package main.groovy.io.peasoup.inv

import org.junit.Test

class InvTest {

    @Test
    void ok() {

        ExpandoMetaClass.enableGlobally()

        def inv = new InvDescriptor()

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


        def digested = inv()

        assert digested.size() == 3
        assert digested[0].name == "my-server"
        assert digested[1].name == "my-webservice"
        assert digested[2].name == "my-app"
    }
}
