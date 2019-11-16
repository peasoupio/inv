package io.peasoup.inv.defaults

import io.peasoup.inv.InvHandler
import io.peasoup.inv.InvInvoker
import io.peasoup.inv.Logger
import org.junit.Before
import org.junit.Test

class HttpTests {

    @Before
    void setup() {
        ExpandoMetaClass.enableGlobally()
        Logger.DebugModeEnabled = true
    }

    @Test
    void get() {

        def inv = new InvHandler()

        InvInvoker.invoke(inv, new File("./defaults/http/inv.groovy"))

        inv {
            require inv.HTTP into '$http'

            step {
                assert $http.newRequest("https://google.com")
                            .send()
                            .valid()
            }
        }

        def (digested, ok) =  inv()

        assert digested
        assert ok
    }

    @Test
    void post() {

        def inv = new InvHandler()

        InvInvoker.invoke(inv, new File("./defaults/http/inv.groovy"))

        inv {
            require inv.HTTP into '$http'

            step {

                def data = "My super duper hyper mega data"

                def req = $http.newRequest("https://postman-echo.com/post")
                        .method("POST")
                        .parameter("value1", data)
                        .send()

                assert req.valid()
                assert req.toText()
                assert req.toJson().form.value1 == data
            }
        }

        def (digested, ok) =  inv()

        assert digested
        assert ok
    }
}
