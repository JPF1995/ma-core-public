//>>built
define("dojo/number",["./_base/lang","./i18n","./i18n!./cldr/nls/number","./string","./regexp"],function(p,m,s,q,k){var f={};p.setObject("dojo.number",f);f.format=function(b,a){a=p.mixin({},a||{});var c=m.normalizeLocale(a.locale),c=m.getLocalization("dojo.cldr","number",c);a.customs=c;c=a.pattern||c[(a.type||"decimal")+"Format"];return isNaN(b)||Infinity==Math.abs(b)?null:f._applyPattern(b,c,a)};f._numberPatternRE=/[#0,]*[#0](?:\.0*#*)?/;f._applyPattern=function(b,a,c){c=c||{};var e=c.customs.group,
d=c.customs.decimal;a=a.split(";");var g=a[0];a=a[0>b?1:0]||"-"+g;if(-1!=a.indexOf("%"))b*=100;else if(-1!=a.indexOf("\u2030"))b*=1E3;else if(-1!=a.indexOf("\u00a4"))e=c.customs.currencyGroup||e,d=c.customs.currencyDecimal||d,a=a.replace(/\u00a4{1,3}/,function(b){return c[["symbol","currency","displayName"][b.length-1]]||c.currency||""});else if(-1!=a.indexOf("E"))throw Error("exponential notation not supported");var h=f._numberPatternRE,g=g.match(h);if(!g)throw Error("unable to find a number expression in pattern: "+
a);!1===c.fractional&&(c.places=0);return a.replace(h,f._formatAbsolute(b,g[0],{decimal:d,group:e,places:c.places,round:c.round}))};f.round=function(b,a,c){c=10/(c||10);return(c*+b).toFixed(a)/c};if(0==(0.9).toFixed()){var r=f.round;f.round=function(b,a,c){var e=Math.pow(10,-a||0),d=Math.abs(b);if(!b||d>=e)e=0;else if(d/=e,0.5>d||0.95<=d)e=0;return r(b,a,c)+(0<b?e:-e)}}f._formatAbsolute=function(b,a,c){c=c||{};!0===c.places&&(c.places=0);Infinity===c.places&&(c.places=6);a=a.split(".");var e="string"==
typeof c.places&&c.places.indexOf(","),d=c.places;e?d=c.places.substring(e+1):0<=d||(d=(a[1]||[]).length);0>c.round||(b=f.round(b,d,c.round));b=String(Math.abs(b)).split(".");var g=b[1]||"";a[1]||c.places?(e&&(c.places=c.places.substring(0,e)),e=void 0!==c.places?c.places:a[1]&&a[1].lastIndexOf("0")+1,e>g.length&&(b[1]=q.pad(g,e,"0",!0)),d<g.length&&(b[1]=g.substr(0,d))):b[1]&&b.pop();d=a[0].replace(",","");e=d.indexOf("0");-1!=e&&(e=d.length-e,e>b[0].length&&(b[0]=q.pad(b[0],e)),-1==d.indexOf("#")&&
(b[0]=b[0].substr(b[0].length-e)));var d=a[0].lastIndexOf(","),h,n;-1!=d&&(h=a[0].length-d-1,a=a[0].substr(0,d),d=a.lastIndexOf(","),-1!=d&&(n=a.length-d-1));a=[];for(d=b[0];d;)e=d.length-h,a.push(0<e?d.substr(e):d),d=0<e?d.slice(0,e):"",n&&(h=n,delete n);b[0]=a.reverse().join(c.group||",");return b.join(c.decimal||".")};f.regexp=function(b){return f._parseInfo(b).regexp};f._parseInfo=function(b){b=b||{};var a=m.normalizeLocale(b.locale),a=m.getLocalization("dojo.cldr","number",a),c=b.pattern||a[(b.type||
"decimal")+"Format"],e=a.group,d=a.decimal,g=1;if(-1!=c.indexOf("%"))g/=100;else if(-1!=c.indexOf("\u2030"))g/=1E3;else{var h=-1!=c.indexOf("\u00a4");h&&(e=a.currencyGroup||e,d=a.currencyDecimal||d)}a=c.split(";");1==a.length&&a.push("-"+a[0]);a=k.buildGroupRE(a,function(a){a="(?:"+k.escapeString(a,".")+")";return a.replace(f._numberPatternRE,function(a){var c={signed:!1,separator:b.strict?e:[e,""],fractional:b.fractional,decimal:d,exponent:!1};a=a.split(".");var l=b.places;1==a.length&&1!=g&&(a[1]=
"###");1==a.length||0===l?c.fractional=!1:(void 0===l&&(l=b.pattern?a[1].lastIndexOf("0")+1:Infinity),l&&void 0==b.fractional&&(c.fractional=!0),!b.places&&l<a[1].length&&(l+=","+a[1].length),c.places=l);a=a[0].split(",");1<a.length&&(c.groupSize=a.pop().length,1<a.length&&(c.groupSize2=a.pop().length));return"("+f._realNumberRegexp(c)+")"})},!0);h&&(a=a.replace(/([\s\xa0]*)(\u00a4{1,3})([\s\xa0]*)/g,function(a,c,e,d){a=k.escapeString(b[["symbol","currency","displayName"][e.length-1]]||b.currency||
"");c=c?"[\\s\\xa0]":"";d=d?"[\\s\\xa0]":"";return!b.strict?(c&&(c+="*"),d&&(d+="*"),"(?:"+c+a+d+")?"):c+a+d}));return{regexp:a.replace(/[\xa0 ]/g,"[\\s\\xa0]"),group:e,decimal:d,factor:g}};f.parse=function(b,a){var c=f._parseInfo(a),e=RegExp("^"+c.regexp+"$").exec(b);if(!e)return NaN;var d=e[1];if(!e[1]){if(!e[2])return NaN;d=e[2];c.factor*=-1}d=d.replace(RegExp("["+c.group+"\\s\\xa0]","g"),"").replace(c.decimal,".");return d*c.factor};f._realNumberRegexp=function(b){b=b||{};"places"in b||(b.places=
Infinity);"string"!=typeof b.decimal&&(b.decimal=".");if(!("fractional"in b)||/^0/.test(b.places))b.fractional=[!0,!1];"exponent"in b||(b.exponent=[!0,!1]);"eSigned"in b||(b.eSigned=[!0,!1]);var a=f._integerRegexp(b),c=k.buildGroupRE(b.fractional,function(a){var c="";a&&0!==b.places&&(c="\\"+b.decimal,c=Infinity==b.places?"(?:"+c+"\\d+)?":c+("\\d{"+b.places+"}"));return c},!0),e=k.buildGroupRE(b.exponent,function(a){return a?"([eE]"+f._integerRegexp({signed:b.eSigned})+")":""}),a=a+c;c&&(a="(?:(?:"+
a+")|(?:"+c+"))");return a+e};f._integerRegexp=function(b){b=b||{};"signed"in b||(b.signed=[!0,!1]);"separator"in b?"groupSize"in b||(b.groupSize=3):b.separator="";var a=k.buildGroupRE(b.signed,function(a){return a?"[-+]":""},!0),c=k.buildGroupRE(b.separator,function(a){if(!a)return"(?:\\d+)";a=k.escapeString(a);" "==a?a="\\s":"\u00a0"==a&&(a="\\s\\xa0");var c=b.groupSize,f=b.groupSize2;return f?(a="(?:0|[1-9]\\d{0,"+(f-1)+"}(?:["+a+"]\\d{"+f+"})*["+a+"]\\d{"+c+"})",0<c-f?"(?:"+a+"|(?:0|[1-9]\\d{0,"+
(c-1)+"}))":a):"(?:0|[1-9]\\d{0,"+(c-1)+"}(?:["+a+"]\\d{"+c+"})*)"},!0);return a+c};return f});
//@ sourceMappingURL=number.js.map