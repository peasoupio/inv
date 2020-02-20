package io.peasoup.inv.defaults

import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.InvHandler
import org.junit.Test

class HttpTests {

    @Test
    void get() {

        def executor = new InvExecutor()
        executor.read(new File("./defaults/http/inv.groovy"))

        new InvHandler(executor).call {

            require inv.HTTP into '$http'

            step {
                assert $http.newRequest("https://google.com")
                            .send()
                            .valid()
            }
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void post() {

        def executor = new InvExecutor()
        executor.read(new File("./defaults/http/inv.groovy"))

        new InvHandler(executor).call {

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

        def report = executor.execute()
        assert report.isOk()
    }
}
