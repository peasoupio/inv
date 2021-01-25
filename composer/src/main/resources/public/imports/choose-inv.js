Vue.component('choose-inv', {
    template: `
<div class="columns">

    <div class="column is-6">
        <panel v-model="ownersSettings" v-if="ownersSettings" />
    </div>

    <div class="column is-6" v-show="idPanels.length > 0">
        <div v-for="(settings, index) in idPanels">
            <panel v-model="idPanels[index]" />
        </div>
    </div>

    <div class="modal is-active" v-show="whoBroughtMeTree">
        <div class="modal-background"></div>
        <div class="modal-content" style="width: 80%">
            <div class="box" v-click-outside="closeWhoBroughtMe">
                <h1 class="title is-3">Who brought: {{whoBroughtMe}}</h1>
                <div v-if="!whoBroughtMeTree || !whoBroughtMeTree.length">
                    <p class="has-text-centered" style="padding: 1em"><strong>Nobody</strong>. Maybe it is staged without any requirement?</p>
                </div>
                <div class="columns is-multiline is-centered">
                    <div class="column is-4 is-primary" v-for="branch in whoBroughtMeTree">
                        <div class="notification">
                            <p class="title is-3">Required: <strong>{{branch[0].value}}</strong></p>
                            <hr />
                            <div class="has-text-centered" v-for="(leaf, index) in branch">
                                <p v-if="leaf.owner">
                                    <span class="tag is-primary" v-if="leaf.value == whoBroughtMe"><strong>{{leaf.value}}</strong></span>
                                    <span class="tag is-primary" v-else>{{leaf.value}}</span>
                                </p>
                                <p v-if="leaf.id">
                                    <span class="tag" style="white-space: normal; display: flex; height: fit-content">{{leaf.value}}</span>
                                </p>
                                <p v-if="index < branch.length - 1">
                                    <span class="icon">
                                      <i class="fas fa-angle-down"></i>
                                    </span>
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            updateKey: 0,
            ownersSettings: {},
            idPanels: [],
            whoBroughtMe: null,
            whoBroughtMeTree: null
        }
    },
    methods: {
        createOwnersSettings: function() {
            var vm = this

            var defaultIcon = 'fa-file-signature'
            var owners = []
            var filters = {
                owner: '',
                staged: false
            }

            var settings = {
                help: `
<ul>
<li>Each <i>INV</i> are under <i>owner</i>.</li>
<li>An <i>owner</i> has many <i>statements</i> obtained from a previous execution.</li>
<li>A <i>Statement</i> is either a <strong>require</strong> or a <strong>broadcast</strong>.</li>
<li>When staged, only <strong>broadcasts</strong> of the <i>owner</i> are shown to the right</li>
</ul>
`,
                clickable: true,
                title: "INV(s)",
                icon: defaultIcon,
                total: owners.length,
                updateKey: 0,
                elements: [],
                options: [{
                    label: "Toggle staged only",
                    toggle: false,
                    click: function() {
                        filters.staged = !filters.staged
                        filter()
                    }
                }]
            }

            var fetch = function() {
                owners = []

                axios.get(vm.value.api.links.run.owners).then(response => {
                    // Create elements from owner's data
                    response.data.forEach(function(owner) {
                        owners.push(vm.createOwnerElement(owner, fetch))
                    })

                    filter()
                })
                .catch(err => {
                    vm.$bus.$emit('toast', `error:Failed to <strong>fetch broadcast owners</strong>!`)
                })
            }

            var filter = function() {
                settings.elements = []

                // Filter elements
                settings.elements = owners.filter(function(owner) {
                    if (filters.owner && owner.data.owner.indexOf(filters.owner) < 0) return
                    if (filters.staged && (owner.data.stagedBy + owner.data.requiredBy) == 0) return

                    return true
                })

                // Sort by label
                settings.elements.sort(compareValues('label'))

                // Update settings total
                settings.total = settings.elements.length
            }

            settings.click = function(element) {
                vm.addIdSettings(element)
            }

            settings.filter = function(word) {
                filters.owner = word
                filter()
            }

            // Initial fetch
            fetch()

            return settings
        },
        createOwnerElement: function(owner, fetch) {
            var vm = this

            var element = {
              data: owner,
              active: false,
              label: owner.owner,
              clickable: true,
              links: owner.links,
              icon: '',
              options: []
            }

            // Check if active
            if (owner.requiredBy > 0 || owner.stagedBy > 0) {
                element.active = true
                element.subLabel = owner.requiredBy + owner.stagedBy
            }

            // Add 'stage' option (if links available)
            if (!element.active && element.links.stage) {
                element.options.push({
                  label: 'Stage',
                  click: function(e) {
                     if (e.sending)
                         return

                     e.icon = 'fa-spinner fa-pulse'
                     e.sending = true

                     axios.post(e.links.stage).then(response => {
                        fetch()
                        e.icon = ''
                        e.sending = false
                     }).catch(err => {
                        e.icon = ''
                        e.sending = false

                        vm.$bus.$emit('toast', `error:Failed to <strong>stage INV</strong>!`)
                     })
                  }
                })
            }

            // Add 'unstage' (if links available)
            if (owner.stagedBy > 0  && element.links.unstage) {
                element.options.push({
                    label: 'Un-stage',
                    click: function(e) {
                        e.icon = 'fa-spinner fa-pulse'
                        e.sending = true

                        axios.post(e.links.unstage).then(response => {
                            fetch()
                            e.icon = ''
                            e.sending = false
                        }).catch(err => {
                          e.icon = ''
                          e.sending = false

                          vm.$bus.$emit('toast', `error:Failed to <strong>un-stage INV</strong>!`)
                       })
                    }
                })
            }

            // Add 'who brought me'
            if (element.active) {
                element.options.push({
                    label: 'Who brought me?',
                    click: function(e) {
                        axios.get(e.links.tree).then(response => {
                            vm.whoBroughtMe = e.label
                            vm.whoBroughtMeTree = response.data
                        })
                    }
                })
            }

            return element
        },
        addIdSettings: function(ownerElement) {
            var vm = this

            var filters = {
                staged: false,
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
<li>Since everything is intertwined, by staging <strong>one broadcast</strong>, you will bring all its <strong>predecessors</strong>.</li>
<li>When the icon <i class="fas fa-lock" aria-hidden="true"></i> is highlighted, the <strong>broadcast</strong> has been staged as a <strong>predecessor</strong>.</li>
<li>A <strong>predecessor cannot be un-staged</strong>. You <strong>must</strong> remove the staged <strong>successor(s)</strong>.</li>
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
                            active: inv.staged || inv.required,
                            clickable: !inv.required,
                            inv: inv
                        })
                    })

                    settings.total = response.data.count
                    settings.activeCount = response.data.requiredByAssociation + response.data.staged
                    settings.elements.sort(compareValues('label'))
                })
            }

            settings.close = function() {
                vm.idPanels.splice(vm.idPanels.indexOf(settings), 1)
            }

            settings.click = function(element) {
                if (element.sending)
                    return

                if (!element.inv.staged && element.inv.links.stage) {
                    element.icon = 'fa-spinner fa-pulse'
                    element.sending = true

                    axios.post(element.inv.links.stage, vm.filters).then(response => {
                        fetch()

                        ownerElement.active = true
                        if (!ownerElement.subLabel)
                            ownerElement.subLabel = 1
                        else
                            ownerElement.subLabel++
                    }).catch(err => {
                        element.sending = false

                        vm.$bus.$emit('toast', `error:Failed to <strong>stage broadcast</strong>!`)
                    })
                } else if(element.inv.links.unstage) {
                    element.icon = 'fa-spinner fa-pulse'
                    element.sending = true

                    axios.post(element.inv.links.unstage, vm.filters).then(response => {
                        fetch()

                        ownerElement.subLabel--

                        if (ownerElement.subLabel == 0) {
                            ownerElement.active = false
                            ownerElement.subLabel = null
                        }
                    }).catch(err => {
                        element.sending = false

                        vm.$bus.$emit('toast', `error:Failed to <strong>unstage broadcast</strong>!`)
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
        },
        closeWhoBroughtMe: function() {
            this.whoBroughtMeTree = null
        }
    },
    mounted: function() {
        this.ownersSettings = this.createOwnersSettings()
    }

})