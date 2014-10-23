Gx = function() {
    this.data = [];
};

Gx.prototype = {
    
    appendClass : function(el, cls, deep) {
        if (el.className) {
            if (deep) {
                var cn = el.childNodes;
                for (var i = 0, length = cn.length; i < length; i++) {
                    this.appendClass(cn[i], cls, deep);
                }
            }
            if (el.className.length == 0) {
                el.className = cls;
            } else {
                var notfound = true;
                var cnames = el.className.split(' ');
                for (var i = 0, length = cnames.length; i < length; i++) {
                    if (cnames[i] == cls) {
                        notfound = false;
                        break;
                    }
                }
                if (notfound) {
                    el.className += ' ' + cls;
                }
            }
        }
    },
    
    removeClass : function(el, cls, deep) {
        if (el.className) {
            if (deep) {
                var cn = el.childNodes;
                for (var i = 0, length = cn.length; i < length; i++) {
                    this.removeClass(cn[i], cls, deep);
                }
            }
            var cnames = el.className.split(' ');
            for (var i = 0, length = cnames.length; i < length; i++) {
                if (cnames[i] == cls) {
                    cnames[i] = '';
                    break;
                }
            }
            el.className = cnames.join(' ');
        }
    },
    
    on : function(index) {
        var cls = this.data[index].cls;
        var set = this.data[index].set;
        for (var i = 0, length = set.length; i < length; i++) {
            var el = document.getElementById(set[i]);
            if (el) {
                this.appendClass(el, cls, false);
            }
        }
    },
    
    off : function(index) {
        var cls = this.data[index].cls;
        var set = this.data[index].set;
        for (var i = 0, length = set.length; i < length; i++) {
            var el = document.getElementById(set[i]);
            if (el) {
                this.removeClass(el, cls, false);
            }
        }
    }
    
};
