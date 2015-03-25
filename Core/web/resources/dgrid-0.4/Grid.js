//>>built
define("dgrid-0.4/Grid","dojo/_base/declare dojo/_base/kernel dojo/on dojo/has put-selector/put ./List ./util/misc dojo/_base/sniff".split(" "),function(n,q,v,w,h,x,r){function p(a,b){b&&b.nodeType&&a.appendChild(b)}n=n(x,{columns:null,cellNavigation:!0,tabableHeader:!0,showHeader:!0,column:function(a){return"object"!==typeof a?this.columns[a]:this.cell(a).column},listType:"grid",cell:function(a,b){if(a.column&&a.element)return a;a.target&&a.target.nodeType&&(a=a.target);var c;if(a.nodeType){do{if(this._rowIdToObject[a.id])break;
var e=a.columnId;if(e){b=e;c=a;break}a=a.parentNode}while(a&&a!==this.domNode)}if(!c&&"undefined"!==typeof b){var d=this.row(a);if(e=d&&d.element)for(var e=e.getElementsByTagName("td"),f=0;f<e.length;f++)if(e[f].columnId===b){c=e[f];break}}if(null!=a)return{row:d||this.row(a),column:b&&this.column(b),element:c}},createRowCells:function(a,b,c,e){var d=h("table.dgrid-row-table[role\x3dpresentation]"),f=9>w("ie")?h(d,"tbody"):d,g,t,n,u,p,s,k,m,l,q;c=c||this.subRows;t=0;for(n=c.length;t<n;t++){s=c[t];
g=h(f,"tr");s.className&&h(g,"."+s.className);u=0;for(p=s.length;u<p;u++){k=s[u];m=k.id;l=k.field?".field-"+r.escapeCssIdentifier(k.field,"-"):"";(q="function"===typeof k.className?k.className(e):k.className)&&(l+="."+q);l=h(a+".dgrid-cell"+(m?".dgrid-column-"+r.escapeCssIdentifier(m,"-"):"")+l.replace(/ +/g,".")+"[role\x3d"+("th"===a?"columnheader":"gridcell")+"]");l.columnId=m;if(m=k.colSpan)l.colSpan=m;if(m=k.rowSpan)l.rowSpan=m;b(l,k);g.appendChild(l)}}return d},left:function(a,b){a.element||
(a=this.cell(a));return this.cell(this._move(a,-(b||1),"dgrid-cell"))},right:function(a,b){a.element||(a=this.cell(a));return this.cell(this._move(a,b||1,"dgrid-cell"))},_defaultRenderCell:function(a,b,c){if(this.formatter){var e=this.formatter,d=this.grid.formatterScope;c.innerHTML="string"===typeof e&&d?d[e](b,a):this.formatter(b,a)}else null!=b&&c.appendChild(document.createTextNode(b))},renderRow:function(a,b){var c=this,e=this.createRowCells("td",function(d,e){var g=a;e.get?g=e.get(a):"field"in
e&&"_item"!==e.field&&(g=g[e.field]);e.renderCell?p(d,e.renderCell(a,g,d,b)):c._defaultRenderCell.call(e,a,g,d,b)},b&&b.subRows,a);return h("div[role\x3drow]\x3e",e)},renderHeader:function(){var a=this,b=this.headerNode,c=b.childNodes.length;for(b.setAttribute("role","row");c--;)h(b.childNodes[c],"!");c=this.createRowCells("th",function(a,b){var c=b.headerNode=a,g=b.field;g&&(a.field=g);if(b.renderHeaderCell)p(c,b.renderHeaderCell(c));else if("label"in b||b.field)c.appendChild(document.createTextNode("label"in
b?b.label:b.field));!1!==b.sortable&&(g&&"_item"!==g)&&(a.sortable=!0,a.className+=" dgrid-sortable")},this.subRows&&this.subRows.headerRows);this._rowIdToObject[c.id=this.id+"-header"]=this.columns;b.appendChild(c);this._sortListener&&this._sortListener.remove();this._sortListener=v(c,"click,keydown",function(c){if("click"===c.type||32===c.keyCode||!w("opera")&&13===c.keyCode){var d=c.target,f,g,h;do if(d.sortable){h=[{property:f=d.field||d.columnId,descending:(g=a.sort[0])&&g.property===f&&!g.descending}];
f={bubbles:!0,cancelable:!0,grid:a,parentType:c.type,sort:h};v.emit(c.target,"dgrid-sort",f)&&(a._sortNode=d,a.set("sort",h));break}while((d=d.parentNode)&&d!==b)}})},resize:function(){var a=this.headerNode.firstChild,b=this.contentNode,c;this.inherited(arguments);b.style.width="";if(b&&a&&(c=a.offsetWidth)>b.offsetWidth)b.style.width=c+"px"},destroy:function(){this._destroyColumns();this._sortListener&&this._sortListener.remove();this.inherited(arguments)},_setSort:function(){this.inherited(arguments);
this.updateSortArrow(this.sort)},_findSortArrowParent:function(a){var b=this.columns,c;for(c in b){var e=b[c];if(e.field===a)return e.headerNode}},updateSortArrow:function(a,b){this._lastSortedArrow&&(h(this._lastSortedArrow,"\x3c!dgrid-sort-up!dgrid-sort-down"),h(this._lastSortedArrow,"!"),delete this._lastSortedArrow);b&&(this.sort=a);if(a[0]){var c=a[0].property,e=a[0].descending,c=this._sortNode||this._findSortArrowParent(c),d;delete this._sortNode;c&&(c=c.contents||c,d=this._lastSortedArrow=
h("div.dgrid-sort-arrow.ui-icon[role\x3dpresentation]"),d.innerHTML="\x26nbsp;",c.insertBefore(d,c.firstChild),h(c,e?".dgrid-sort-down":".dgrid-sort-up"),this.resize())}},styleColumn:function(a,b){return this.addCssRule("#"+r.escapeCssIdentifier(this.domNode.id)+" .dgrid-column-"+r.escapeCssIdentifier(a,"-"),b)},_configColumns:function(a,b){var c=[],e=b instanceof Array;r.each(b,function(d,f){"string"===typeof d&&(b[f]=d={label:d});!e&&!d.field&&(d.field=f);f=d.id=d.id||(isNaN(f)?f:a+f);this._configColumn&&
(this._configColumn(d,b,a),f=d.id);e&&(this.columns[f]=d);d.grid=this;"function"===typeof d.init&&(q.deprecated("colum.init","Column plugins are being phased out in favor of mixins for better extensibility. column.init may be removed in a future release."),d.init());c.push(d)},this);return e?b:c},_destroyColumns:function(){var a=this.subRows,b=a&&a.length,c,e,d,f;this.cleanup();for(c=0;c<b;c++){e=0;for(f=a[c].length;e<f;e++)d=a[c][e],"function"===typeof d.destroy&&(q.deprecated("colum.destroy","Column plugins are being phased out in favor of mixins for better extensibility. column.destroy may be removed in a future release."),
d.destroy())}},configStructure:function(){var a=this.subRows,b=this._columns=this.columns;this.columns=!b||b instanceof Array?{}:b;if(a)for(b=0;b<a.length;b++)a[b]=this._configColumns(b+"-",a[b]);else this.subRows=[this._configColumns("",b)]},_getColumns:function(){return this._columns||this.columns},_setColumns:function(a){this._destroyColumns();this.subRows=null;this.columns=a;this._updateColumns()},_setSubRows:function(a){this._destroyColumns();this.subRows=a;this._updateColumns()},_updateColumns:function(){this.configStructure();
this.renderHeader();this.refresh();this._lastCollection&&this.renderArray(this._lastCollection);this._started&&(this.sort.length?this.updateSortArrow(this.sort):this.resize())}});n.appendIfNode=p;return n});
//# sourceMappingURL=Grid.js.map