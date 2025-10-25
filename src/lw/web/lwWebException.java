package lw.web;

// 自定义Web异常：用于在业务代码中抛出明确的错误码与原因
// 初学者提示：当遇到参数错误或权限不足等情况时，抛出该异常，框架会统一输出JSON错误响应
public class lwWebException extends Exception{

// 错误码（例如：400 参数错误，401 未登录，403 无权限，404 未找到，-1 未知异常）
public int error;
// 错误原因的文本描述
public String reason;

public lwWebException(int error,String reason) {
	this.error=error;
	this.reason=reason;
}
@Override
public String getMessage() {
	// 返回异常信息。当前实现仅返回 "reason:"+错误码，用于演示；实际可改为返回 reason 文本
	return "reason:"+error;
}

}