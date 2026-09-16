<%@ page contentType="text/html;charset=UTF-8" %><%
    // Welcome file — sends the root context straight to the products list
    // served by ProductsController (@WebServlet("/products/*")).
    response.sendRedirect(request.getContextPath() + "/products");
%>
