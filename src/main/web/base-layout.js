Vue.component('base-layout', {
    template: `
<div class="columns">
    <div class="column is-2 sidebar">
        <p class="title is-1">Inv</p>
        <p class="subtitle is-3">Composer</p>
        <hr />
        <div>
            <ul class="steps is-vertical">

                <li v-for="(step, index) in steps"
                    class="steps-segment"
                    v-bind:class=" { 'is-active': currentStep == step.template } ">

                    <span href="#" class="steps-marker">{{index + 1}}</span>
                    <div class="steps-content">
                        <p class="is-size-4"><a v-on:click="currentStep = step.template">{{step.name}}</a></p>
                        <p>{{step.description}}</p>
                    </div>
                </li>

            </ul>
        </div>
    </div>
    <div class="column is-10" v-if="ready()">
        <component v-bind:is="currentStep" v-model="value" />
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            currentStep: 'configure',
            steps: [
                { name: 'Configure', template: 'configure', description: 'Configure your parameters and scms' },
                { name: 'Choose', template: 'choose', description: 'Choose your INVs' },
                { name: 'Review', template: 'review', description: 'Review everything' },
                { name: 'Install', template: 'install', description: 'Generate and install your freshly new INV ecosystem' }
            ],
            loadState: {
                scm: false,
                availables: false
            }
        }
    },
    methods: {
        ready: function() {
            return this.loadState.scms && this.loadState.availables
        }
    },
    created: function() {
        var vm = this

        axios.get('/runs').then(response => {

            var data = response.data

            var broadcasts = {}
            var requires = {}

            for (const [owner, node] of Object.entries(data.graph.nodes)) {

                var matchedBroadcast = ''

                data.graph.broadcasts.forEach(function(broadcast) {
                    if (broadcast.owner != owner)
                        return

                    matchedBroadcast = broadcast.id
                });

                var matchedSCM = ''

                data.files.forEach(function(file) {
                    if (file.inv != owner)
                        return

                    matchedSCM = file.scm
                });


                var name = ''
                var id = ''

                if (matchedBroadcast != '') {
                   name = matchedBroadcast.split(' ')[0].replace('[', '').replace(']', '')
                   id = matchedBroadcast.split(' ')[1].replace('[', '').replace(']', '')
                }


                vm.value.availables.push({
                    chosen: false,
                    broughtBySomeone: false,
                    owner: owner,
                    name: name,
                    id: id,
                    scm: matchedSCM,
                    required: data.flattenedEdges[owner]
                })

            }

            vm.loadState.availables = true
            vm.$forceUpdate()
        });

        axios.get('/scms').then(response => {
            vm.value.scms = response.data

            vm.loadState.scms = true
            vm.$forceUpdate()
        });
    }
})