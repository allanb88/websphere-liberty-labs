<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Products</title>
    <style>
        body { font-family: -apple-system, "Segoe UI", Arial, sans-serif; margin: 2rem auto; max-width: 800px; color: #1a2230; }
        h1 { margin-bottom: .25rem; }
        .subtitle { color: #667; margin-top: 0; font-size: .9rem; }
        .error { background: #fdecea; border: 1px solid #e39; color: #a33; padding: .6rem .9rem; border-radius: 6px; margin-bottom: 1rem; }
        .table { border: 1px solid #dde; border-radius: 8px; overflow: hidden; margin-bottom: 2rem; }
        .row { display: grid; grid-template-columns: 48px 1fr 110px 90px 150px; align-items: center; gap: .5rem; padding: .5rem .75rem; border-bottom: 1px solid #eef; }
        .row:last-child { border-bottom: none; }
        .row.header { background: #f4f6fa; font-size: .75rem; text-transform: uppercase; letter-spacing: .04em; color: #667; font-weight: 600; }
        .row input { width: 100%; box-sizing: border-box; padding: .3rem .4rem; border: 1px solid #cdd; border-radius: 4px; font-size: .9rem; }
        .row .id { color: #889; font-variant-numeric: tabular-nums; }
        .actions { display: flex; gap: .4rem; }
        button { cursor: pointer; border: 1px solid #cdd; background: #fff; padding: .35rem .7rem; border-radius: 5px; font-size: .85rem; }
        button[value="PUT"] { border-color: #3d5a80; color: #3d5a80; }
        button[value="DELETE"] { border-color: #b3312c; color: #b3312c; }
        button[type="submit"]:hover { filter: brightness(0.97); }
        .new-product { border: 1px solid #dde; border-radius: 8px; padding: 1rem 1.25rem; }
        .new-product h2 { margin-top: 0; font-size: 1.1rem; }
        .new-product label { display: block; font-size: .8rem; color: #667; margin-bottom: .2rem; }
        .new-product .fields { display: grid; grid-template-columns: 2fr 1fr 1fr auto; gap: .75rem; align-items: end; }
        .new-product input { width: 100%; box-sizing: border-box; padding: .4rem .5rem; border: 1px solid #cdd; border-radius: 4px; }
        .new-product button { background: #3d5a80; color: #fff; border-color: #3d5a80; padding: .5rem 1rem; }
    </style>
</head>
<body>

<h1>Products</h1>
<p class="subtitle">products-ui (:9080) &rarr; products-api (:9081) &rarr; PostgreSQL</p>

<c:if test="${not empty errorMessage}">
    <p class="error">${errorMessage}</p>
</c:if>

<div class="table">
    <div class="row header">
        <span>ID</span><span>Name</span><span>Price</span><span>Stock</span><span>Actions</span>
    </div>
    <c:forEach var="p" items="${products}">
        <form class="row" method="post" action="${pageContext.request.contextPath}/products">
            <span class="id">${p.id}</span>
            <input type="hidden" name="id" value="${p.id}">
            <input type="text" name="name" value="${p.name}" required>
            <input type="text" name="price" value="${p.price}" required>
            <input type="number" name="stock" value="${p.stock}" min="0" required>
            <span class="actions">
                <button type="submit" name="_method" value="PUT">Save</button>
                <button type="submit" name="_method" value="DELETE"
                        onclick="return confirm('Delete ${p.name}?');">Delete</button>
            </span>
        </form>
    </c:forEach>
    <c:if test="${empty products}">
        <div class="row"><span colspan="5" style="grid-column: 1 / -1; color: #889;">No products yet.</span></div>
    </c:if>
</div>

<div class="new-product">
    <h2>New product</h2>
    <form method="post" action="${pageContext.request.contextPath}/products">
        <div class="fields">
            <div>
                <label for="name">Name</label>
                <input type="text" id="name" name="name" required>
            </div>
            <div>
                <label for="price">Price</label>
                <input type="text" id="price" name="price" placeholder="0.00" required>
            </div>
            <div>
                <label for="stock">Stock</label>
                <input type="number" id="stock" name="stock" min="0" required>
            </div>
            <button type="submit" name="_method" value="POST">Create</button>
        </div>
    </form>
</div>

</body>
</html>
