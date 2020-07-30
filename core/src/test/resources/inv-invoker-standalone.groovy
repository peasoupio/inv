import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.InvInvoker

InvExecutor executor = new InvExecutor()
InvInvoker.invoke(executor, new File("./src/test/resources/inv-invoker-script.groovy"))

def report = executor.execute()
assert report.isOk()