var LW = {};

/* 输出日志 */
LW.log = function(msg)
{
	 try {   console.log(msg);     } catch (err) {}
};

/* 封装jQuery，内部作个数检查 (防止selector写错) 
    输入可以为selector, Element, jQuery对象
 * */
function LW$ ( selector ) 
{
	var a=selector;
	if(a==null) throw "LW$: selector不得为null!"
	if(a.constructor != jQuery) a = $(a);
	if(a.length == 0) throw "LW$: jQuery没有选中对象!是否传错了selector? 输入为:" + selector;
	return a;
}
LW.getContextPath=function()
{
	var pathName=window.document.location.pathname;  
    var path= pathName.substring(0,pathName.substr(1).indexOf('/')+1)+'/';
    return path;
};
/* 获取元素 , 返回的是Element对象
 * 可以输入:String, jQuery, Element
 * 
 */
LW.getElement = function( selector )
{
	var e = selector;
	if(e==null) throw "LW.getElement(): selector不得为null!"
	if(e.constructor == String) e = $(e);
	if(e.constructor == jQuery)
	{
		if(e.length == 0) throw "jQuery没有选中对象!是否传错了selector?"
		e = e[0];
	}
	return e;
}

/* 获取查询参数 */
LW.getUrlParam = function (name, defValue) 
{
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) 
    	return unescape(r[2]);
    else
    	return defValue;
}



/* RESTful 调用的封装 
 *  示例  LW.rest("uri_of_rest_service", req, function(ans){
 * 	
 * });
 */

// 默认错误处理
LW.restErrHandler = function(error, reason)
{	
	alert(reason);
};

// serviceUri 服务名, req 请求参象 , okHandler 应答数据处理函数
// errorHandler 处理处理器, 可以为null
LW.rest = function (serviceUri, req, okHandler, errHandler)
{
	jQuery.ajax({				
		url: serviceUri, 			
		method: "POST", 
		contentType:"application/json",
		processData: false,	//序列化
		data: JSON.stringify(req), 
		dataType: 'json',
		success: function(ans){
			if(ans.error != 0)
			{			
				// 接下来，先用第4个参数(用户自己处理器处理)				 
				if(errHandler != null)
					errHandler( ans.error, ans.reason);
				else
				// 如果未提供第4个参数，则进入默认处理(alert)
					LW.restErrHandler (ans.error, ans.reason); // 默认错误处理
			}				
			else
			{
				okHandler(ans.data);
			}				
		},
		//LW.rest('AddStudent',req,function(data){},function(){})
		error: function( jqXHR, textStatus, errorThrown){
			if(errHandler != null)
				errHandler( -1, "网络错误: " + textStatus);
			else
				console.log("网络错误: " + textStatus);
		}
	});	
}

/* JSONP 调用 */
LW.jsonp = function(URI, req, resultHanlder)
{
	jQuery.ajax({				
			url: URI,	
			method: "GET", // get方式
			dataType: "jsonp", // 1: jsonp 
			//jsonpCallback: "callback",
			data: req, // 参数
			success: resultHanlder
	});	
}

/* localStorage /sessionStorage */
/* value可以为 String , Number, Boolean , Object, Array*/
LW.putStorage = function (storage, key, value)
{
	if(value == null) return ;
	if(value.constructor == String)	value = "S#" + value;
	else if(value.constructor == Number) value = "N#" + value;
	else if(value.constructor == Boolean) value = "B#" + value;
	else value =  "J#" + JSON.stringify(value);	
	storage.setItem(key, value);
}
LW.getStorage = function(storage, key ,defValue)
{	
	var value = storage.getItem(key);
	if(value == null) return defValue ;	
	var type = value.substr(0,2);
	value = value.substr(2);
	if(type=="S#") return value;
	if(type=="N#") return Number(value);
	if(type=="B#") return value=="true";
	if(type=="J#") return JSON.parse(value);
	return defValue;
}
LW.putLocal = function(key, value)
{
	LW.putStorage(localStorage, key, value);
}
LW.getLocal = function(key, defValue)
{
	try{
		return LW.getStorage(localStorage, key, defValue);
	}catch(err){
		LW.log('getLocal:格式不对?' + key);
		return defValue;
	}
}
/* sessionStorage */
LW.putSession = function(key, value)
{
	LW.putStorage(sessionStorage, key, value);
}
LW.getSession = function(key, defValue)
{
	try{
		return LW.getStorage(sessionStorage, key, defValue);
	}catch(err){
		LW.log('getSession:格式不对?' + key);
		return defValue;	
	}
}

/* 动态加载页面 */
LW.loadPage = function( container, url)
{
	// container可以是选择器字符串
	if(container.constructor == String)	 
		container = $(container);
		
	$.get(url, function(content){
		container.html( content);
	});
}


/*  用法: 
 *   var template = $(".template").html();
 *   var tmpl = new LWTemplate(template) ;
 *   var data = { ... }
 *   var html = tmpl.replace(data);
 * 
 */
function LWTemplate(template)
{
	this.map = null;;
	this.template = template;
	
	this.compile = function()
	{
		this.map = {};
		
		var r = new RegExp("\\{#.*?\\}", "g");

		var result;
		while ((result = r.exec( this.template )) != null)  
		{
			//LW.log( result[0] );
			var match = result[0]; // {#xxx}
			var key = match.substr(2, match.length - 3); // 不允许带空格  
			//var key = key1.trim();
			this.map[key] = new RegExp("\\{#" + key + "\\}", "g");
			//LW.log(varName);
		}
		
		return this;
	};
	
	this.replace= function( data )
	{
		// 第一次运行时编译
		if(this.map == null) this.compile();
		
		var html = this.template;
		for( var key in this.map)
		{
			var regex = this.map[key];
			var value = data[ key];
			if(value != null)
				html = html.replace( regex, value);
		}
		return html;
	};
}

/* map 定义 */
/* 可以按ID查询的表 */
function LWMap()
{
	this.array = [];
	
	this.put = function(id, obj)
	{
		/* 检查重复, 如果已经存在直接替换 */
		for(var i=0; i<this.array.length;i++)
		{
			var e = this.array[i];
			if(e.id == id) 
			{
				e.obj = obj;
				return;
			}
		}
		/* 添加新的项 */
		var e = {};
		e.id = id;
		e.obj = obj;
		this.array.push( e );
	};
	
	this.get = function(id)
	{
		for(var i=0; i<this.array.length;i++)
		{
			var e = this.array[i];
			if(e.id == id) 
				return e.obj;
		}
		return null;
	};
	
	/* 遍历 : callback: 如果要continue就return true, 否则返回false */
	this.each = function ( callback )
	{
		for(var i=0; i<this.array.length;i++)
		{
			var e = this.array[i];
			if(false == callback (e.id, e.obj ) ) break;
		}
	};
	
	this.remove = function ( id )
	{
		for(var i=0; i<this.array.length;i++)
		{
			var e = this.array[i];
			if(e.id == id)
				this.array.splice(i, 1);
		}
	};
	
	this.size = function()
	{
		return this.array.length;
	};
	
	this.clear = function()
	{
		this.array = [];
	};
	
	this.values = function()
	{
		var values = [];
		for(var i=0; i<this.array.length;i++)
		{
			var e = this.array[i];
			values.push(e.obj);
		}
		return values;
	};
	this.ids = function()
	{
		var result = [];
		for(var i=0; i<this.array.length;i++)
		{
			var e = this.array[i];
			result.push(e.id);
		}
		return result;
	};
}

/* 以逗号分隔的ID列表 */
function LWIdList ()
{
	this.ids = [];	
	
	this.aa = function (str)
	{
		if(str==null || str.length==0) return this;
		var sss = str.split(",");
		for(var i=0; i<sss.length; i++)
		{
			var it = sss[i];
			if(it.length > 0 && ! this.contains( it ))
			{				
				this.ids.push(it);
			}
		}
		return this;
	};
	this.at = function(index)
	{
		if(this.ids.length == 0) return null;
		return this.ids[index];
	};
	this.contains = function (id)
	{
		for(var i=0; i<this.ids.length; i++)
		{
			if( id == this.ids[i]) return true;
		}
		return false;
	};
	this.size = function ()
	{
		return this.ids.length;
	};
	this.toString = function()
	{
		return this.ids.join(",");
	}
}


/*
 * 上传工具类 
 */
function LWUploader()
{
	this.fileButton = null; // 原生DOM
	this.file = null; // 要上传的文件
	this.uploadUrl = null; 
	this.status = 0; // 0, 1, 100, -1(失败), -2(canceled)
	this.progress = 0; // 0-100
	this.response = "{}"; // 上传完毕后服务器的返回值, 一般应该是一个JSON字符串
	this.enableLog = true; // 是否显示内部打印
	
	// 输入参数可以是:Selector / jQuery对象 / 原生DOM
	this.setButton = function(fileButton)
	{
		// 如果输入参数是 Selector
		if(fileButton.constructor == String) fileButton = $(fileButton);
		
		// 如果输入参数是jQuery对象, 则转成DOM
		if(fileButton.constructor == jQuery)
		{
			if(fileButton.length==0) throw ("你的jQuery选择器写错了!请检查选择器!");
			fileButton = fileButton[0];
		}
		
		// 先看原来有没有绑定, 如果已经绑定了一个uploader，则返回原有对象
		if(fileButton.uploader != null)  return fileButton.uploader;
		
		// 创建新的uploader
		this.fileButton = fileButton;
		
		// 把上下文存放到DOM元素里
		this.fileButton.uploader = this;
		
		// 添加回调，确保只添加一次
//			this.fileButton.removeEventListener("change", this.onFileChanged);
//			this.fileButton.addEventListener("change", this.onFileChanged);
		this.fileButton.addEventListener("change", function(){
			var ctx = this.uploader;
			var fileButton = this;
			if(fileButton.files.length == 0) return;		
			
			var file = fileButton.files[0];
			ctx.log("select file: " + file.name);
			fileButton.value = ''; // 清空选择
			ctx.setFile(file);
			ctx.startUpload( );
		});
		
		return this;
	};
	
	this.setUploadUrl = function(url)
	{
		this.uploadUrl = url;
		return this;
	};
	
	// 事件处理 : 'start' 'progress' 'complete' 'error' 'abort'
	this.processEvent = function( event )
	{		
	};
	
	// 是否接受上传, true：可以上传; false:不能上传
	this.beforeUpload = function()
	{
		return true;
	};
	
	this.setLogEnabled  = function( enabled)
	{
		this.enableLog = enabled;
		return this;
	};
	
	// 设置文件 (并不立即启动)
	this.setFile = function(file)
	{
		this.file = file;
		return this;
	};
		
	// 打开文件对话框，让用户选择文件
	this.openFileDialog = function()
	{
		if(this.fileButton == null) throw ("尚未初始化file button! 请调用 setButton() 进行设置!");

		$(this.fileButton).click();	
	};

	// 外部可以直接送进来一个 File 对象
	this.startUpload = function( )
	{
		if(this.uploadUrl == null) throw ("尚未设置uploadUrl,无法上传! 请调用 setUploadUrl()进行设置!");
		if(this.file == null) throw ("尚未设置file! 请调用openFileDialog()打开文件选择对话框!或者调用setFile()传一个File对象!");
		
		var file = this.file;
		// 上传测试: 是否允许上传
		if( ! this.beforeUpload())
		{
			this.log("不满足上传条件 ! " + file.name);
			return;
		}
		
		this.log("开始上传: " + file.name);

	   	var formData = new FormData();
		formData.append('file', file); // 'file' 为HTTP Post里的字段名, file 对浏览器里的File对象
				    
	    var formRequest = new XMLHttpRequest();
	    formRequest.ctx = this;
	    formRequest.upload.ctx = this;
	    
	    formRequest.upload.addEventListener("progress", this.evt_upload_progress, false);
	    formRequest.addEventListener("load", this.evt_upload_complete, false);
	    formRequest.addEventListener("error", this.evt_upload_failed, false);
	    formRequest.addEventListener("abort", this.evt_upload_cancel, false);		
	
		this.processEvent('start');
		formRequest.open("POST", this.uploadUrl );
	    formRequest.send(formData);
	    
	    this.formRequest = formRequest; /* 保存这个上传者对象, 用于调用其abort()函数 */
	   	this.status = 1;		   		   	
	   	return this;
	};
	
	// 取消上传
	this.cancelUpload = function()
	{		
		if(this.formRequest != null)
		{
			try{
				this.formRequest.abort(); 
    			this.formRequest = null;
    			this.status = -2;
			}catch(err)
			{
				LW.log("取消上传时出错 ：" + err);
			}
    	}
	};
	
	this.log = function(msg)
	{
		if(!this.enableLog) return;
		try {   console.log(msg);     } catch (err) {}
	};
	
	//////////////////////////////
	
	this.evt_upload_progress = function (evt) 
	{
		var ctx = this.ctx;
	    if (evt.lengthComputable)
	    {
	    	ctx.progress = Math.round(evt.loaded * 100 / evt.total);		    	
	    	ctx.log ("上传进度: " + ctx.progress);		
	    	ctx.processEvent('progress');
	    }	        
	};
	this.evt_upload_complete = function (evt)
	{
		var ctx = this.ctx;
		if(evt.loaded == 0)
		{
			ctx.status = -1;
			ctx.log ("上传失败!" + ctx.file.name);
			ctx.processEvent('error');
		}
		else
		{
			ctx.status = 100;
	    	ctx.response = evt.target.responseText;
	   		ctx.log (ctx.response); 
	   		ctx.processEvent('complete');
		}			
	};		 
	this.evt_upload_failed = function (evt) 
	{			
		var ctx = this.ctx;
		ctx.status = -1;
		ctx.log ("上传出错"); 
		ctx.processEvent('error');
	};
	this.evt_upload_cancel = function (evt) 
	{
		var ctx = this.ctx;
		ctx.status = -2;
		ctx.log( "上传中止!");	
		ctx.processEvent('abort');
	};
}

/* 表单自动提取
 * 
 */
function LWForm(container)
{
	this.div = LW.getElement (container); // Element
	this.trimmed = true;
	
	// 自动设置值: 如果是控件,则调用val() ; 如果是普通元素, 则调用 html()
	this.set = function( obj )
	{
		for(var name in obj)
		{
			var value = obj[name];
			var e = this.field( name, false);
			if(e.length == 0) continue; // 无此元素
			if( this.isControl(e))	
				e.val( value );
			else	
				e.html( value);
		}
	};
	
	// 自动获取所有输入的值
	this.get = function()
	{
		var result = {};
		var eee = $('[name]',this.div); // 获取所有有name属性的元素
		for(var e of eee)
		{
			if (this.isControl( e))
			{
				var name = $(e).attr('name');
				var v = $(e).val();
				if(true == this.trimmed && v.constructor==String) 
					v = v.trim(); // 去除空白字符
				result[name] = v;
			}
		}
		return result;
	};
	
	// 设置是否trim (默认true)
	this.trim = function(yes)
	{
		this.trimmed = yes;
		return this;
	};
	
	// 寻找name=xxx的字段; showErr为false时, 不抛出异常
	this.field = function ( name , showErr)
	{
		var selector = "[name='"+ name + "']" ;
		var result = $(selector ,this.div);			
		if(showErr== null || true == showErr)
		{
			if(result.length == 0) throw '无此Field: name=' + name;
		}
		return 	result;
	};
	
	// 寻找子元素
	this.child = function ( selector , showErr)
	{
		var result = $(selector ,this.div);		
		if(showErr== null || true == showErr)
		{
			if(result.length == 0) throw '无此Field: name=' + elementName;
		}
		return result;	
	};	
	
	// 判断元素是否为控件
	this.isControl = function ( e )
	{
		if(e.constructor == String) e = $(e); // String -> jQuery
		if(e.constructor == jQuery) e = e[0]; // jQuery -> Element
			
		var tagName = e.tagName.toLowerCase();
		return tagName == 'input' || tagName == 'select' || tagName == 'textarea';
	};
	
	// 子元素绑定事件处理
	this.handleEvent = function(child, event, handler)
	{
		var e = this.child( child );
		e.unbind( event );
		e.on (event, handler);
	}
}

/* 对话框 */
function LWDialog(container)
{
	this.div = LW.getElement(container);
	this.form = new LWForm(container);
	this.trimmed = true;
	
	this.show = function()
	{
		$(this.div).show();
		
		// 所有class='close'的添加事件处理
		var owner = this;
		
		$('[role=close]', this.div).unbind('click'); // 绑定多次绑定
		$('[role=close]', this.div).on('click', function(){
			owner.hide();
		});		
	};	
	
	this.hide = function()
	{
		$(this.div).hide();
	};
	// 设置字段的值
	this.set = function( obj )
	{
		this.form.set(obj);	
	};
	// 取字段的值
	this.get = function( )
	{
		return this.form.get();
	};
	
	// 设置是否trim (默认true)
	this.trim = function(yes)
	{
		this.form.trim(yes);
		return this;
	};
	
	this.child = function(selector)
	{
		return this.form.child(selector);
	};
	this.field = function(name)
	{
		return this.form.field(name);
	};
	this.handleEvent = function(child, event, handler)
	{
		this.form.handleEvent(child, event, handler);
	}
}

/* 弹出式窗口 */
LW.popup = function ( settings )
{
	var anchor = $(settings.anchor)[0];
	var popup =  $(settings.popup)[0];
	
	// 确保只初始化一次
	if(anchor.pupup_init_flag == true) return;
	anchor.pupup_init_flag = true;
	
	function fadeOut(target)
	{
		setTimeout(function(){
			if(target.popup_focus != true)
			{
				target.style.display = 'none';
			}					
		}, 50);
	}

	popup.addEventListener('mouseenter', function(){
		console.log('popup ...mouse in...');
		this.popup_focus = true;
	});
	
	popup.addEventListener('mouseleave', function(){
		this.popup_focus = false;
		console.log('popup ...mouse out ...');
		fadeOut( popup);				
	});			
		
	anchor.addEventListener('mouseenter', function(){			
		 // offsetLeft, offsetTop, offsetWidth, offsetHeight
		popup.style.display = 'block';
		popup.style.left = this.offsetLeft + "px";
		popup.style.top = ( this.offsetTop + this.offsetHeight + 7) + "px"; /* 控制锚点与面板的间距*/
	});
	
	anchor.addEventListener('mouseleave', function(){					
		fadeOut(popup); 
	});					
}


/* 通用渲染与分页工具
 * 目标：将各页面的列表/表格渲染与分页逻辑抽象，统一在 LW 下复用。
 * 不改变现有页面行为；提供轻量 API，便于后续逐步迁移。
 *
 * 核心函数：
 * - LW.paginate(list, pageNo, pageSize) -> {items,pageNo,pageSize,total,totalPages}
 * - LW.renderTable(table, rows, columns)
 * - LW.renderList(list, items, renderItem)
 * - LW.attachPager(container, state, onChange)
 * - LW.renderPagerTable(opts)
 */
LW.paginate = function(list, pageNo, pageSize){
	pageNo = Number(pageNo)||1; pageSize = Number(pageSize)||10;
	var arr = Array.isArray(list)? list.slice() : [];
	var total = arr.length;
	var totalPages = Math.max(1, Math.ceil(total / pageSize));
	if(pageNo < 1) pageNo = 1;
	if(pageNo > totalPages) pageNo = totalPages;
	var start = (pageNo - 1) * pageSize;
	var items = arr.slice(start, start + pageSize);
	return { items: items, pageNo: pageNo, pageSize: pageSize, total: total, totalPages: totalPages };
};

// 渲染表格：columns 形如 [{key:'name', title:'课程', formatter:(row,val)=>...}, ...]
LW.renderTable = function(table, rows, columns){
	var $t = LW$(table);
	var thead = $('thead', $t);
	var tbody = $('tbody', $t);
	if(thead.length === 0){ $t.prepend('<thead></thead>'); thead = $('thead',$t); }
	if(tbody.length === 0){ $t.append('<tbody></tbody>'); tbody = $('tbody',$t); }
	if(Array.isArray(columns) && columns.length > 0){
		var headerHtml = '<tr>' + columns.map(function(c){ return '<th>' + (c.title || c.key || '') + '</th>'; }).join('') + '</tr>';
		thead.html(headerHtml);
	}
	tbody.empty();
	(rows || []).forEach(function(row){
		var tds = (columns || []).map(function(c){
			var v = row[c.key];
			if(typeof c.formatter === 'function'){ v = c.formatter(row, v); }
			if(v == null) v = '';
			return '<td>' + v + '</td>';
		}).join('');
		tbody.append('<tr>' + tds + '</tr>');
	});
	return $t;
};

// 渲染列表：renderItem(it, idx) 返回 HTML 字符串或 jQuery
LW.renderList = function(list, items, renderItem){
	var $ul = LW$(list);
	$ul.empty();
	(items || []).forEach(function(it, idx){
		var html = typeof renderItem === 'function' ? renderItem(it, idx) : ('<li>' + (it==null? '' : it) + '</li>');
		$ul.append(html);
	});
	return $ul;
};

// 简易分页器：在容器内生成 上一页/下一页 + 信息；回调 onChange(state)
LW.attachPager = function(container, state, onChange){
	var $c = LW$(container);
	var s = state || { pageNo:1, pageSize:10, total:0, totalPages:1 };
	var html = '<div class="lw-pager">' +
		'<button class="lw-prev">上一页</button>' +
		'<span class="lw-info">第 '+ s.pageNo +' / '+ s.totalPages +' 页，共 '+ s.total +' 条</span>' +
		'<button class="lw-next">下一页</button>' +
		'</div>';
	$c.html(html);
	function refresh(){
		$('.lw-info', $c).text('第 '+ s.pageNo +' / '+ s.totalPages +' 页，共 '+ s.total +' 条');
		$('.lw-prev', $c).prop('disabled', s.pageNo <= 1);
		$('.lw-next', $c).prop('disabled', s.pageNo >= s.totalPages);
	}
	$('.lw-prev', $c).off('click').on('click', function(){
		if(s.pageNo > 1){ s.pageNo--; refresh(); if(typeof onChange === 'function') onChange(s); }
	});
	$('.lw-next', $c).off('click').on('click', function(){
		if(s.pageNo < s.totalPages){ s.pageNo++; refresh(); if(typeof onChange === 'function') onChange(s); }
	});
	refresh();
	return { refresh: refresh, state: s };
};

/* 组合：查询 + 客户端分页 + 表格渲染
 * opts: {
 *   table: '#tbl',          // 表格选择器
 *   pager: '#pager',        // 分页容器选择器（可选）
 *   queryFn: function(ok, err){ ... }, // 拉取完整列表（或已分页）
 *   pageSize: 10,           // 每页大小
 *   columns: [ ... ],       // 表格列定义
 *   transform: function(list){ return list; } // 可选，对数据做适配
 * }
 */
LW.renderPagerTable = function(opts){
	var pageSize = Number(opts.pageSize)||10;
	var state = { pageNo: 1, pageSize: pageSize, total: 0, totalPages: 1 };
	function load(){
		opts.queryFn(function(list){
			var data = (typeof opts.transform==='function') ? opts.transform(list) : list;
			var pg = LW.paginate(data, state.pageNo, state.pageSize);
			state.total = pg.total; state.totalPages = pg.totalPages; state.pageNo = pg.pageNo;
			LW.renderTable(opts.table, pg.items, opts.columns);
			if(opts.pager) LW.attachPager(opts.pager, state, function(s){ state = s; load(); });
		}, function(e, r){ LW.log('renderPagerTable error: '+ r); });
	}
	load();
	return { reload: load, state: state };
};
