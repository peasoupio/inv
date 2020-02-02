Vue.component('choose', {
    template: `
<div>
    <div class="columns">
        <div class="column is-2">
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
                        { label: 'Simple', description: 'Select from a simple view of INVs', template: 'choose-select-simple'},
                        { label: 'Complex', description: 'Select from a more detailed view of INVs', template: 'choose-select-complex'},
                        { label: 'By SCM', description: 'For more experienced user, choose by SCM', template: 'choose-scm'},
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

Vue.component('choose-not-available', {
    template: `
<div class="content">
    <p class="has-text-warning has-text-centered title is-4">Oopsy, it seems no run information is available.</p>
    <p>Have you considered the following?</p>
    <ul>
        <li>The error basically means no "run.txt" is available within Composer's reach</li>
        <li>The first-time usage does not provide a default "run.txt" file. You must start the process by choosing SCM. Take a look at "By SCM". Upon successful completion, "run.txt" will be available.</li>
        <li>Check on your filesystem under INV_HOME path or the current Composer execution path if "run.txt" is present</li>

    </ul>
</div>
`
})

Vue.component('choose-select-simple', {
    template: `
<div>

    <choose-not-available v-if="notAvailable"></choose-not-available>
    <div class="columns" v-else>

        <div class="column is-5">
            <panel v-model="ownersSettings" v-show="owners.length > 0"/>
        </div>

        <div class="column is-7" v-show="idPanels.length > 0">
            <div v-for="(settings, index) in idPanels">
                <panel v-model="idPanels[index]" />
            </div>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            notAvailable: false,
            owners: [],
            idPanels: []
        }
    },
    computed: {
        ownersSettings: {
            get() {
                var vm = this

                var defaultIcon = 'fa-file-signature'

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
                    title: "Owners",
                    icon: defaultIcon,
                    total: vm.owners.length,
                    elements: vm.owners
                }

                settings.pick = function(element) {
                    if (element == undefined)
                        return

                    if (element.sending)
                        return

                    element.icon = 'fa-spinner fa-pulse'
                    element.sending = true

                    axios.post(element.links.stage).then(response => {
                        location.reload()
                    })
                }

                settings.click = function(element) {
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
                title: "Ids of: " + ownerElement.label,
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
    created: function() {
        var vm = this

        axios.get(vm.value.api.links.run.owners).then(response => {
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

                vm.owners.push(element)
            })

            vm.owners.sort(compareValues('label'))
        })
        .catch(response => {
            vm.notAvailable = true
        })
    }

})
