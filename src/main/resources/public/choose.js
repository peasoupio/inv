Vue.component('choose', {
    template: `
<div>
    <div class="columns">
        <div class="column is-2" style="position: sticky">
            <tab-tiles v-model="tabTilesSettings" />
        </div>

        <div class="column" v-show="currentTab.template">
            <component v-bind:is="currentTab.template" v-model="value" />
        </div>
    </div>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            currentTab: {}
        }
    },
    computed: {
        tabTilesSettings: {
            get() {
                var vm = this

                return {
                    tabs: [
                        { label: 'Simple', description: 'Select from a simple view of INVs', template: 'choose-select-simple', isWhat: 'link'},
                        { label: 'Complex', description: 'Select from a more detailed view of INVs', template: 'choose-select-complex', isWhat: 'danger'},
                        { label: 'Resume', description: 'A detailed view of your current selections', template: 'choose-summary', isWhat: 'primary'},
                    ],
                    tabSet: function(tab) {
                        vm.currentTab = tab
                        vm.$forceUpdate()
                    }
                }
            }
        }
    }
})

Vue.component('choose-select-simple', {
    template: `
<div class="columns">

    <div class="column is-4">
        <panel v-model="ownersSettings" v-show="owners.length > 0"/>
    </div>

    <div class="column is-8" v-show="idPanels.length > 0">
        <div v-for="(settings, index) in idPanels">
            <panel v-model="idPanels[index]" />
        </div>
    </div>


</div>
`,
    props: ['value'],
    data: function() {
        return {
            owners: [],
            idPanels: []
        }
    },
    computed: {
        ownersSettings: {
            get() {
                var vm = this

                var settings = {
                    help: `
<ul>
<li>Each <i>INV</i> are under <i>owner</i>.</li>
<li>An <i>owner</i> has many <i>network valuables</i> obtained from a previous execution.</li>
<li><i>Network Valuables</i> are <strong>require</strong> and <strong>broadcast</strong> statements of the <i>INV</i>.</li>
<li>When selected, only <strong>broadcasts</strong> of the <i>owner</i> are shown to the right</li>
</ul>
`,
                    clickable: true,
                    title: "Owners",
                    icon: 'fa-file-signature',
                    total: vm.owners.length,
                    elements: vm.owners
                }

                settings.click = function(element) {
                    if (element.active)
                        return

                    element.active = true

                    vm.addIdSettings(element)
                }

                settings.filter = function(word) {
                    if (word == '') {
                        settings.elements = vm.owners
                        settings.total = vm.owners.length
                    }

                    var filtered = vm.owners.filter(function(owner) {
                        return owner.label.indexOf(word) > -1
                    })

                    filtered.sort(compareValues('label'))

                    settings.elements = filtered
                    settings.total = filtered.length
                }

                return settings
            }
        },

    },
    methods: {

        addIdSettings: function(element) {
            var vm = this

            var filters = {
                selected: false,
                required: false,
                owner: element.label,
                id: '',
                step: 10,
                from: 0
            }

            var settings = {
                help: `
<ul>
<li>Each <i>broadcasts</i> have a <strong>name</strong> and an </strong>id</strong>.</li>
<li>The <strong>name</strong> is highlighted into a tag (to the left) like this one: <span class="tag is-link">MyName</span></li>
<li>The <strong>id</strong> is the unique identifier for its <strong>name</strong>.</li>
<li>Per example, for <pre>[Endpoint] context:"/my-api"</pre>, <strong>name</strong> is <pre>Endpoint</pre> and <strong>id</strong> is <pre>context:"/my-api"</pre></li>
<li>When the icon <i class="fas fa-fingerprint" aria-hidden="true"></i> is highlighted, the <strong>broadcast</strong> (and its <i>INV</i>) is staged for the next execution.</li>
<li>Since everything is intertwined, by selecting <strong>one broadcast</strong>, you will bring all its <strong>predecessors</strong>.</li>
<li>When the icon <i class="fas fa-lock" aria-hidden="true"></i> is highlighted, the <strong>broadcast</strong> has been selected as a <strong>predecessor</strong>.</li>
<li>A <strong>predecessor cannot be un-selected</strong>. You <strong>must</strong> remove the selected <strong>successor(s)</strong>.</li>
</ul>
`,
                clickable: true,
                title: "Ids of: " + element.label,
                icon: 'fa-fingerprint',
                total: 0,
                activeCount: 0,
                elements: []
            }

            var fetch = function() {
                axios.post(vm.value.api.links.run.search, filters).then(response => {
                    settings.elements = []

                    response.data.nodes.forEach(function(inv) {
                        settings.elements.push({
                            icon: '',
                            label: inv.id,
                            subLabel: inv.name,
                            active: inv.selected || inv.required,
                            clickable: !inv.required,
                            inv: inv
                        })
                    })

                    settings.total = response.data.count
                    settings.activeCount = response.data.requiredByAssociation + response.data.selected
                    settings.elements.sort(compareValues('label'))
                })
            }

            settings.close = function() {
                vm.idPanels.splice(vm.idPanels.indexOf(settings), 1)
                element.active = false
            }

            settings.click = function(element) {
                if (element.sending)
                    return

                element.icon = 'fa-spinner fa-pulse'
                element.sending = true

                if (!element.inv.selected) {
                    axios.post(element.inv.links.stage, vm.filters).then(response => {
                        fetch()
                    })
                } else {
                    axios.post(element.inv.links.unstage, vm.filters).then(response => {
                        fetch()
                    })
                }
            }

            settings.refresh = function(from) {
                filters.from = from
                fetch()
            }

            settings.filter = function(word) {
                filters.id = word
                fetch()
            }

            fetch()
            vm.idPanels.push(settings)
        }
    },
    created: function() {
        var vm = this

        axios.get(vm.value.api.links.run.owners).then(response => {

            response.data.forEach(function(owner) {
                 vm.owners.push({
                    active: false,
                    label: owner.owner,
                    clickable: true
                 })
            })

            vm.owners.sort(compareValues('label'))
        })
    }

})
