Vue.component('choose', {
    template: `
<div>
    <div class="columns">
        <div class="column is-2" style="width: 20%">
            <tab-tiles v-model="tabTilesSettings" v-if="value.setup.firstTime != undefined" />
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
                        { label: 'INV', description: 'Select INVs by their name', template: 'choose-select-simple', disabled: vm.value.setup.firstTime },
                        { label: 'Broadcast', description: 'Select INVs by their specific broadcast(s)', template: 'choose-select-complex', disabled: vm.value.setup.firstTime },
                        { label: 'SCM', description: 'Select INVs by their specific SCM', template: 'choose-scm'},
                        //{ label: 'Resume', description: 'A detailed view of your current selections', template: 'choose-summary'}

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

    <div class="column is-5">
        <panel v-model="ownersSettings" v-if="ownersSettings" />
    </div>

    <div class="column is-7" v-show="idPanels.length > 0">
        <div v-for="(settings, index) in idPanels">
            <panel v-model="idPanels[index]" />
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            ownersSettings: {},
            updateKey: 0,
            idPanels: []
        }
    },
    methods: {
        createOwnersSettings: function() {
            var vm = this

            var defaultIcon = 'fa-file-signature'
            var owners = []
            var filters = {
                owner: ''
            }

            var settings = {
                help: `
<ul>
<li>Each <i>INV</i> are under <i>owner</i>.</li>
<li>An <i>owner</i> has many <i>statements</i> obtained from a previous execution.</li>
<li>A <i>Statement</i> is either a <strong>require</strong> or a <strong>broadcast</strong>.</li>
<li>When selected, only <strong>broadcasts</strong> of the <i>owner</i> are shown to the right</li>
</ul>
`,
                clickable: true,
                title: "INV(s)",
                icon: defaultIcon,
                total: owners.length,
                updateKey: 0,
                elements: []
            }

            var fetch = function() {

                axios.post(vm.value.api.links.run.owners, filters).then(response => {
                    settings.elements = []

                    response.data.forEach(function(owner) {
                        var element = {
                          active: false,
                          label: owner.owner,
                          clickable: true,
                          pickable: true,
                          links: owner.links,
                          icon: ''
                        }

                        if (owner.requiredBy > 0 || owner.selectedBy > 0) {
                            element.active = true
                            element.subLabel = owner.requiredBy + owner.selectedBy
                        }

                        settings.elements.push(element)
                    })

                    // Update settings total
                    settings.total = settings.elements.length

                    // Sort by label
                    settings.elements.sort(compareValues('label'))
                })
                .catch(response => {
                })
            }

            settings.pick = function(element) {
                if (element == undefined)
                    return

                if (element.sending)
                    return

                element.icon = 'fa-spinner fa-pulse'
                element.sending = true

                axios.post(element.links.stage).then(response => {
                    fetch()
                })
            }

            settings.click = function(element) {
                vm.addIdSettings(element)
            }

            settings.filter = function(word) {
                filters.owner = word
                fetch()
            }

            // Initial fetch
            fetch()

            return settings
        },
        addIdSettings: function(ownerElement) {
            var vm = this

            var filters = {
                selected: false,
                required: false,
                owner: ownerElement.label,
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
                title: "Broadcast(s) of " + ownerElement.label,
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
            }

            settings.click = function(element) {
                if (element.sending)
                    return

                element.icon = 'fa-spinner fa-pulse'
                element.sending = true

                if (!element.inv.selected) {
                    axios.post(element.inv.links.stage, vm.filters).then(response => {
                        fetch()

                        ownerElement.active = true
                        if (!ownerElement.subLabel)
                            ownerElement.subLabel = 1
                        else
                            ownerElement.subLabel++
                    })
                } else {
                    axios.post(element.inv.links.unstage, vm.filters).then(response => {
                        fetch()

                        ownerElement.subLabel--

                        if (ownerElement.subLabel == 0) {
                            ownerElement.active = false
                            ownerElement.subLabel = null
                        }
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
    mounted: function() {
        this.ownersSettings = this.createOwnersSettings()
    }

})
