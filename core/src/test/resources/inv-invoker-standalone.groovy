import io.peasoup.inv.run.InvExecutor

InvExecutor executor = new InvExecutor()
executor.addScript(new File("./src/test/resources/inv-invoker-script.groovy"))

def report = executor.execute()
assert report.isOk()