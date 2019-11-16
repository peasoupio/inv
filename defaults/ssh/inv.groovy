// Visit https://gradle-ssh-plugin.github.io/docs/

@Grab('ch.qos.logback:logback-classic:1.1.2')
@Grab("org.hidetake:groovy-ssh:2.10.1")
import org.hidetake.groovy.ssh.Ssh

inv {

    broadcast inv.SSH using {
        ready {[
            $: {
                def instance = [:]

                instance += [
                    ssh: Ssh.newService(),
                    debugMode: {
                        instance.ssh.runtime.logback(level: 'DEBUG')
                    }
                ]

                return instance
            }
        ]}
    }
}

