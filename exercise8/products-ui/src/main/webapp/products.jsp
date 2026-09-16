<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Products</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        /* Every row plays this once on load, staggered by --i (its position),
           so the table feels like it's assembling itself rather than popping in flat. */
        @keyframes row-in {
            from { opacity: 0; transform: translateY(10px); }
            to   { opacity: 1; transform: translateY(0); }
        }
        /* The row matching ?justAdded=<id> additionally flashes indigo and fades
           back to transparent, so the product you just created is unmistakable. */
        @keyframes just-added-glow {
            0%   { background-color: rgb(199 210 254); }
            100% { background-color: transparent; }
        }
        .row-enter {
            animation: row-in .4s cubic-bezier(.16,1,.3,1) both;
            animation-delay: calc(var(--i, 0) * 45ms);
        }
        .row-just-added {
            animation: row-in .4s cubic-bezier(.16,1,.3,1) both,
                       just-added-glow 2s ease-out .4s both;
        }
    </style>
</head>
<body class="min-h-screen bg-gradient-to-br from-slate-50 via-white to-indigo-50 text-slate-800">
<main class="mx-auto max-w-3xl px-4 py-10">

    <div class="mb-8 flex items-center gap-3">
        <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-600 to-fuchsia-500 text-lg font-bold text-white shadow-lg shadow-indigo-500/30">P</div>
        <div>
            <h1 class="text-2xl font-extrabold tracking-tight text-slate-900">Products</h1>
            <p class="text-sm text-slate-500">
                <span class="font-semibold text-indigo-600">products-ui</span> (:9080)
                &rarr; <span class="font-semibold text-fuchsia-600">products-api</span> (:9081)
                &rarr; <span class="font-semibold text-slate-600">PostgreSQL</span>
            </p>
        </div>
    </div>

    <c:if test="${not empty errorMessage}">
        <p class="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">${errorMessage}</p>
    </c:if>

    <div class="mb-8 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm shadow-slate-200/60">
        <div class="grid grid-cols-[56px_1fr_110px_150px_150px] items-center gap-2 bg-gradient-to-r from-indigo-600 to-fuchsia-600 px-4 py-3 text-xs font-semibold uppercase tracking-wider text-white">
            <span>ID</span><span>Name</span><span>Price</span><span>Stock</span><span>Actions</span>
        </div>
        <c:forEach var="p" items="${products}" varStatus="status">
            <form method="post" action="${pageContext.request.contextPath}/products"
                  style="--i: ${status.index}"
                  class="grid grid-cols-[56px_1fr_110px_150px_150px] items-center gap-2 border-b border-slate-100 px-4 py-2.5 last:border-0 transition-colors hover:bg-indigo-50/70 ${status.index % 2 == 0 ? 'bg-white' : 'bg-slate-50/70'} ${p.id == param.justAdded ? 'row-just-added' : 'row-enter'}">
                <span class="flex items-center gap-1.5 font-mono text-xs text-slate-400">
                    #${p.id}
                    <c:if test="${p.id == param.justAdded}">
                        <span class="rounded-full bg-emerald-100 px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wide text-emerald-700">new</span>
                    </c:if>
                </span>
                <input type="hidden" name="id" value="${p.id}">
                <input type="text" name="name" value="${p.name}" required
                       class="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-slate-800 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100">
                <input type="text" name="price" value="${p.price}" required
                       class="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 font-mono text-slate-800 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100">
                <div class="flex items-center gap-1.5">
                    <span class="h-2 w-2 shrink-0 rounded-full ${p.stock <= 5 ? 'bg-rose-500' : (p.stock <= 25 ? 'bg-amber-400' : 'bg-emerald-500')}"></span>
                    <input type="number" name="stock" value="${p.stock}" min="0" required
                           class="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 font-mono text-slate-800 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100">
                </div>
                <span class="flex gap-1.5">
                    <button type="submit" name="_method" value="PUT"
                            class="rounded-lg border border-indigo-200 bg-indigo-50 px-3 py-1.5 text-xs font-semibold text-indigo-700 transition-colors hover:bg-indigo-100">Save</button>
                    <button type="submit" name="_method" value="DELETE"
                            onclick="return confirm('Delete ${p.name}?');"
                            class="rounded-lg border border-rose-200 bg-rose-50 px-3 py-1.5 text-xs font-semibold text-rose-700 transition-colors hover:bg-rose-100">Delete</button>
                </span>
            </form>
        </c:forEach>
        <c:if test="${empty products}">
            <div class="px-4 py-8 text-center text-sm text-slate-400">No products yet.</div>
        </c:if>
    </div>

    <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm shadow-slate-200/60">
        <h2 class="mb-4 text-base font-bold text-slate-900">New product</h2>
        <form method="post" action="${pageContext.request.contextPath}/products"
              class="grid grid-cols-1 gap-4 sm:grid-cols-[2fr_1fr_1fr_auto] sm:items-end">
            <div>
                <label for="name" class="mb-1 block text-xs font-medium text-slate-500">Name</label>
                <input type="text" id="name" name="name" required
                       class="w-full rounded-lg border border-slate-200 px-3 py-2 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100">
            </div>
            <div>
                <label for="price" class="mb-1 block text-xs font-medium text-slate-500">Price</label>
                <input type="text" id="price" name="price" placeholder="0.00" required
                       class="w-full rounded-lg border border-slate-200 px-3 py-2 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100">
            </div>
            <div>
                <label for="stock" class="mb-1 block text-xs font-medium text-slate-500">Stock</label>
                <input type="number" id="stock" name="stock" min="0" required
                       class="w-full rounded-lg border border-slate-200 px-3 py-2 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100">
            </div>
            <button type="submit" name="_method" value="POST"
                    class="rounded-lg bg-gradient-to-r from-indigo-600 to-fuchsia-600 px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-indigo-500/30 transition hover:brightness-105">Create</button>
        </form>
    </div>

</main>
</body>
</html>
