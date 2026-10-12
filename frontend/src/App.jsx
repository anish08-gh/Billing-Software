import {
  NavLink as RouterNavLink,
  Navigate,
  Route,
  Routes,
  useNavigate,
  useParams,
} from "react-router-dom";
import Login from "./pages/Login.jsx";
import { apiRequest } from "./services/api.js";
import { useEffect, useRef, useState } from "react";
import { generateBillPdf } from "./utils/generateBillPdf.js";

function Dashboard() {
    const navigate = useNavigate();
    const [stats, setStats] = useState(null);
    const [recentBills, setRecentBills] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
      async function loadDashboard() {
        try {
          setLoading(true);
          setError("");

          const [statsData, billsData] = await Promise.all([
            apiRequest("/api/bills/dashboard-stats"),
            apiRequest("/api/bills?page=0&size=5"),
          ]);

          setStats(statsData);
          setRecentBills(
            Array.isArray(billsData) ? billsData : billsData.content ?? []
          );
        } catch (err) {
          setError(err.message || "Failed to load dashboard data.");
        } finally {
          setLoading(false);
        }
      }

      loadDashboard();
    }, []);
return ( <div className="page-content"> <div className="page-heading"> <div> <p className="eyebrow">OVERVIEW</p> <h1>Dashboard</h1> <p className="muted">
Here's what's happening with your business today. </p> </div> <button
                                                                className="primary-button"
                                                                onClick={() => navigate("/bills/new")}
                                                              >+ Create New Bill</button> </div>


  <div className="stats-grid">
    <div className="stat-card">
      <span className="stat-label">Total Revenue</span>
      <h2>
        {stats
          ? `₹${Number(stats.totalRevenue).toLocaleString("en-IN", {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}`
          : "—"}
      </h2>
      <p className="stat-note">From recorded bills</p>
    </div>
    <div className="stat-card">
      <span className="stat-label">Total Bills</span>
      <h2>{stats ? stats.totalBills : "—"}</h2>
      <p className="stat-note">Bills created so far</p>
    </div>
    <div className="stat-card">
      <span className="stat-label">Pending Payments</span>
      <h2>
        {stats
          ? `₹${Number(stats.pendingPayments).toLocaleString("en-IN", {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}`
          : "—"}
      </h2>
      <p className="stat-note">Awaiting collection</p>
    </div>
    <div className="stat-card">
      <span className="stat-label">Outstanding Bills</span>
      <h2>{stats ? stats.outstandingBills : "—"}</h2>
      <p className="stat-note">Need payment follow-up</p>
    </div>
  </div>


<section className="content-card">
  <div className="section-heading">
    <div>
      <h2>Recent Bills</h2>
      <p className="muted">Your latest billing activity</p>
    </div>
    <button
      className="secondary-button"
      onClick={() => navigate("/bills")}
    >
      View All Bills
    </button>
  </div>

  {loading ? (
    <div className="empty-state">
      <p className="muted">Loading recent bills...</p>
    </div>
  ) : error ? (
    <div className="empty-state">
      <h3>Couldn't load recent bills</h3>
      <p className="muted">{error}</p>
    </div>
  ) : recentBills.length === 0 ? (
    <div className="empty-state">
      <div className="empty-icon">₹</div>
      <h3>Your bills will appear here</h3>
      <p className="muted">
        Create your first bill to start tracking sales and payments.
      </p>
      <button
        className="primary-button"
        onClick={() => navigate("/bills/new")}
      >
        Create Your First Bill
      </button>
    </div>
  ) : (
    <div className="table-wrapper">
      <table className="data-table recent-bills-table">
        <thead>
          <tr>
            <th>Bill Number</th>
            <th>Customer</th>
            <th>Total</th>
            <th>Payment Status</th>
            <th>Date</th>
          </tr>
        </thead>
        <tbody>
          {recentBills.map((bill) => (
            <tr key={bill.id}>
              <td>
                <button
                  className="text-button"
                  onClick={() => navigate(`/bills/${bill.id}`)}
                >
                  {bill.billNumber}
                </button>
              </td>
              <td>{bill.customerName}</td>
              <td>
                ₹{Number(bill.totalAmount).toLocaleString("en-IN", {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </td>
              <td>{bill.paymentStatus.replaceAll("_", " ")}</td>
              <td>
                {bill.createdAt
                  ? new Date(bill.createdAt).toLocaleDateString("en-IN")
                  : "—"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )}
</section>
</div>


);
}

function PlaceholderPage({ title, description }) {
return ( <div className="page-content"> <p className="eyebrow">BILLING MANAGEMENT</p> <h1>{title}</h1> <p className="muted">{description}</p> <section className="content-card"> <p>This section will be connected to your billing backend next.</p> </section> </div>
);
}


function AllBillsPage() {
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [searchText, setSearchText] = useState("");
  const navigate = useNavigate();
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function loadBills() {
      setLoading(true);
      setError("");

      try {
        const endpoint = searchText.trim()
          ? `/api/bills/search?q=${encodeURIComponent(searchText.trim())}&page=${page}&size=10`
          : `/api/bills?page=${page}&size=10`;

        const data = await apiRequest(endpoint);

        if (cancelled) return;

        // Supports either a plain array or a Spring Page response.
        const billList = Array.isArray(data)
          ? data
          : data.content ?? [];

        setBills(billList);
        setHasNext(
          Array.isArray(data)
            ? false
            : Boolean(data.last === false)
        );
      } catch (err) {
        if (!cancelled) {
          setError(err.message || "Could not load bills.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadBills();

    return () => {
      cancelled = true;
    };
  }, [page, searchText]);

  function formatCurrency(amount) {
    return `₹${Number(amount ?? 0).toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  }

  function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
      ? "—"
      : date.toLocaleDateString("en-IN", {
          day: "2-digit",
          month: "short",
          year: "numeric",
        });
  }

  return (
    <div className="page-content">
      <p className="eyebrow">BILLING MANAGEMENT</p>
      <h1>All Bills</h1>
      <p className="muted">
        Search, review and manage your customer bills.
      </p>

      <section className="content-card">

<div className="bill-search-bar">
  <input
    type="search"
    placeholder="Search by bill number, customer name or phone..."
    value={searchText}
    onChange={(event) => {
      setSearchText(event.target.value);
      setPage(0);
    }}
    aria-label="Search bills"
  />
</div>
        {loading ? (
          <p>Loading bills...</p>
        ) : error ? (
          <div>
            <p role="alert">{error}</p>
            <button
              type="button"
              className="primary-button"
              onClick={() => setRetryCount((current) => current + 1)}
            >
              Retry
            </button>
          </div>
        ) : bills.length === 0 ? (
          <p>No bills found. Create your first bill to see it here.</p>
        ) : (
          <>
            <div className="bill-items-table-wrapper">
              <table className="bill-items-table">
                <thead>
                  <tr>
                    <th>Bill Number</th>
                    <th>Customer</th>
                    <th>Phone</th>
                    <th>Total</th>
                    <th>Payment Status</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {bills.map((bill) => (
                    <tr key={bill.id}>
                      <td>
                        <button
                          type="button"
                          className="bill-number-link"
                          onClick={() => navigate(`/bills/${bill.id}`)}
                        >
                          {bill.billNumber || "—"}
                        </button>
                      </td>
                      <td>{bill.customerName || "—"}</td>
                      <td>{bill.customerPhone || "—"}</td>
                      <td>{formatCurrency(bill.totalAmount)}</td>
                      <td>{bill.paymentStatus || "—"}</td>
                      <td>{formatDate(bill.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="pagination-controls">
              <button
                type="button"
                className="secondary-button"
                disabled={page === 0}
                onClick={() => setPage((current) => current - 1)}
              >
                Previous
              </button>

              <span>Page {page + 1}</span>

              <button
                type="button"
                className="secondary-button"
                disabled={!hasNext}
                onClick={() => setPage((current) => current + 1)}
              >
                Next
              </button>
            </div>
          </>
        )}
      </section>
    </div>
  );
}


function BillDetailsPage() {
    const navigate = useNavigate();
  const { billId } = useParams();
  const [bill, setBill] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [payments, setPayments] = useState([]);
  const [paymentsLoading, setPaymentsLoading] = useState(true);
  const [paymentsError, setPaymentsError] = useState("");
  const [showPaymentForm, setShowPaymentForm] = useState(false);
  const [paymentAmount, setPaymentAmount] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("CASH");
  const [paymentReference, setPaymentReference] = useState("");
  const [paymentNotes, setPaymentNotes] = useState("");
  const [savingPayment, setSavingPayment] = useState(false);
  const [paymentFormError, setPaymentFormError] = useState("");
  const [isEditing, setIsEditing] = useState(false);
  const [savingEdit, setSavingEdit] = useState(false);
  const [editError, setEditError] = useState("");

  const [editCustomerName, setEditCustomerName] = useState("");
  const [editCustomerPhone, setEditCustomerPhone] = useState("");
  const [editItems, setEditItems] = useState([]);

  const [catalogueItems, setCatalogueItems] = useState([]);
  const [editItemSearch, setEditItemSearch] = useState("");
  const [catalogueLoading, setCatalogueLoading] = useState(false);


  useEffect(() => {
    let cancelled = false;

    async function loadBill() {
      setLoading(true);
      setError("");

      try {
        const data = await apiRequest(`/api/bills/${billId}`);

        if (!cancelled) {
          setBill(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || "Could not load bill details.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadBill();

    return () => {
      cancelled = true;
    };
  }, [billId]);

  useEffect(() => {
    async function loadPayments() {
      try {
        setPaymentsLoading(true);
        setPaymentsError("");

        const data = await apiRequest(
          `/api/bills/${billId}/payments`
        );

        setPayments(data);
      } catch (err) {
        setPaymentsError(
          err.message || "Failed to load payment history."
        );
      } finally {
        setPaymentsLoading(false);
      }
    }

    if (billId) {
      loadPayments();
    }
  }, [billId]);


  async function handleSavePayment(event) {
    event.preventDefault();
    setPaymentFormError("");

    const amount = Number(paymentAmount);

    if (
      !paymentAmount.trim() ||
      !Number.isFinite(amount) ||
      amount <= 0
    ) {
      setPaymentFormError("Enter a payment amount greater than zero.");
      return;
    }

    if (!/^\d{1,10}(\.\d{1,2})?$/.test(paymentAmount)) {
      setPaymentFormError("Payment amount can have at most 2 decimal places.");
      return;
    }

    if (amount > Number(bill.pendingAmount)) {
      setPaymentFormError("Payment cannot exceed the pending balance.");
      return;
    }

    try {
      setSavingPayment(true);

      const updatedBill = await apiRequest(
        `/api/bills/${billId}/payments`,
        {
          method: "POST",
          body: JSON.stringify({
            amount: paymentAmount,
            paymentMethod: paymentMethod,
            paymentReference: paymentReference.trim() || null,
            notes: paymentNotes.trim() || null,
          }),
        }
      );

      setBill(updatedBill);

      const updatedPayments = await apiRequest(
        `/api/bills/${billId}/payments`
      );
      setPayments(updatedPayments);

      setPaymentAmount("");
      setPaymentMethod("CASH");
      setPaymentReference("");
      setPaymentNotes("");
      setShowPaymentForm(false);
    } catch (err) {
      setPaymentFormError(err.message || "Failed to save payment.");
    } finally {
      setSavingPayment(false);
    }
  }

  function handleStartEditing() {
    setEditCustomerName(bill.customerName);
    setEditCustomerPhone(bill.customerPhone);

    setEditItems(
      (bill.items ?? []).map((item) => ({
        itemId: item.itemId,
        itemName: item.itemName,
        unit: item.unit,
        quantity: String(item.quantity),
        unitPrice: Number(item.unitPrice),
      }))
    );

    setEditError("");
    setEditItemSearch("");
    setCatalogueLoading(true);
    setIsEditing(true);

    apiRequest("/api/items")
      .then((items) => {
        setCatalogueItems(items);
      })
      .catch((err) => {
        setEditError(err.message || "Failed to load the item catalogue.");
      })
      .finally(() => {
        setCatalogueLoading(false);
      });
  }

  async function handleSaveEdit(event) {
    event.preventDefault();
    setEditError("");

    if (editItems.length === 0) {
      setEditError("Add at least one item to the bill.");
      return;
    }

    try {
      setSavingEdit(true);

      const updatedBill = await apiRequest(`/api/bills/${billId}`, {
        method: "PUT",
        body: JSON.stringify({
          customerName: editCustomerName.trim(),
          customerPhone: editCustomerPhone.trim(),
          expectedVersion: bill.version,
          items: editItems.map((item) => ({
            itemId: item.itemId,
            quantity: item.quantity,
          })),
        }),
      });

      setBill(updatedBill);
      setIsEditing(false);
    } catch (err) {
      setEditError(
        err.message || "Failed to update bill. Please try again."
      );
    } finally {
      setSavingEdit(false);
    }
  }

  function handleAddEditItem(item) {
    setEditItems((currentItems) => [
      ...currentItems,
      {
        itemId: item.id,
        itemName: item.name,
        unit: item.unit,
        quantity: "1",
        unitPrice: Number(item.price),
      },
    ]);

    setEditItemSearch("");
  }

  const filteredCatalogueItems = catalogueItems.filter((item) => {
    const query = editItemSearch.trim().toLowerCase();

    const matchesSearch =
      item.name?.toLowerCase().includes(query) ||
      item.sku?.toLowerCase().includes(query);

    const alreadyInBill = editItems.some(
      (billItem) => billItem.itemId === item.id
    );

    return matchesSearch && !alreadyInBill && item.active !== false;
  });

  const formatCurrency = (amount) =>
    `₹${Number(amount ?? 0).toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;

  if (loading) {
    return (
      <div className="page-content">
        <p>Loading bill details...</p>
      </div>
    );
  }

  if (error || !bill) {
    return (
      <div className="page-content">
        <h1>Bill Details</h1>
        <p role="alert">{error || "Bill not found."}</p>
        <button
          type="button"
          className="secondary-button"
          onClick={() => navigate("/bills")}
        >
          Back to All Bills
        </button>
      </div>
    );
  }

  return (
    <div className="page-content">
      <p className="eyebrow">BILLING MANAGEMENT</p>
      <h1>Bill Details</h1>
      <p className="muted">Bill Number: {bill.billNumber}</p>

      <section className="content-card">
        {isEditing ? (
          <>
            <h2>Edit Bill</h2>

            <div className="form-group">
              <label htmlFor="editItemSearch">Add Items from Catalogue</label>

              <input
                id="editItemSearch"
                type="text"
                value={editItemSearch}
                onChange={(event) => setEditItemSearch(event.target.value)}
                placeholder="Search by item name or SKU"
                autoComplete="off"
                disabled={catalogueLoading}
              />

              {catalogueLoading && <p>Loading catalogue...</p>}

              {!catalogueLoading && editItemSearch.trim() && (
                <div className="catalogue-search-results">
                  {filteredCatalogueItems.length > 0 ? (
                    filteredCatalogueItems.map((item) => (
                      <button
                        key={item.id}
                        type="button"
                        className="secondary-button"
                        onClick={() => handleAddEditItem(item)}
                      >
                        {item.name} — ₹
                        {Number(item.price).toLocaleString("en-IN", {
                          minimumFractionDigits: 2,
                          maximumFractionDigits: 2,
                        })}
                        {" "}({item.unit})
                      </button>
                    ))
                  ) : (
                    <p>No matching active items found.</p>
                  )}
                </div>
              )}
            </div>

            {editError && (
              <p className="form-error" role="alert">
                {editError}
              </p>
            )}

            <form onSubmit={handleSaveEdit}>
              <div className="form-group">
                <label htmlFor="editCustomerName">Customer Name</label>
                <input
                  id="editCustomerName"
                  type="text"
                  maxLength={150}
                  value={editCustomerName}
                  onChange={(event) =>
                    setEditCustomerName(event.target.value)
                  }
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="editCustomerPhone">Customer Phone</label>
                <input
                  id="editCustomerPhone"
                  type="tel"
                  inputMode="numeric"
                  maxLength={10}
                  pattern="[6-9][0-9]{9}"
                  value={editCustomerPhone}
                  onChange={(event) =>
                    setEditCustomerPhone(event.target.value)
                  }
                  required
                />
              </div>

              <div className="bill-details-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setIsEditing(false)}
                  disabled={savingEdit}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="primary-button"
                  disabled={savingEdit}
                >
                  {savingEdit ? "Saving..." : "Save Changes"}
                </button>
              </div>
            </form>
          </>
        ) : (
          <>
            <h2>Customer Details</h2>
            <p>Name: {bill.customerName}</p>
            <p>Phone: {bill.customerPhone}</p>
          </>
        )}
        <p>Date: {bill.createdAt ? new Date(bill.createdAt).toLocaleString("en-IN") : "—"}</p>

        <h2>Items</h2>
        <div className="bill-items-table-wrapper">
          <table className="bill-items-table">
            <thead>
              <tr>
                <th>Item</th>
                <th>Quantity</th>
                <th>Unit Price</th>
                <th>Total</th>
                {isEditing && <th>Action</th>}
              </tr>
            </thead>

<tbody>
  {(isEditing ? editItems : (bill.items ?? [])).map((item) => (
    <tr key={item.id ?? item.itemId}>
      <td>{item.itemName}</td>

      <td>
        {isEditing ? (
          <input
            type="number"
            min="0.001"
            step="0.001"
            value={item.quantity}
            onChange={(event) => {
              const quantity = event.target.value;

              setEditItems((currentItems) =>
                currentItems.map((editItem) =>
                  editItem.itemId === item.itemId
                    ? { ...editItem, quantity }
                    : editItem
                )
              );
            }}
            required
          />
        ) : (
          item.quantity
        )}
      </td>

      <td>{formatCurrency(item.unitPrice)}</td>
      <td>
        {formatCurrency(
          isEditing
            ? Number(item.unitPrice) * Number(item.quantity)
            : item.totalAmount
        )}
      </td>
      {isEditing && (
        <td>
          <button
            type="button"
            className="secondary-button"
            onClick={() =>
              setEditItems((currentItems) =>
                currentItems.filter(
                  (editItem) => editItem.itemId !== item.itemId
                )
              )
            }
          >
            Remove
          </button>
        </td>
      )}

    </tr>
  ))}
</tbody>
          </table>
        </div>

        <div className="bill-summary">
          <p>Subtotal: <strong>{formatCurrency(bill.subtotal)}</strong></p>
          <p>Grand Total: <strong>{formatCurrency(bill.totalAmount)}</strong></p>
          <p>Amount Paid: <strong>{formatCurrency(bill.amountPaid)}</strong></p>
          <p>Balance Due: <strong>{formatCurrency(bill.pendingAmount)}</strong></p>
          <p>Payment Status: <strong>{bill.paymentStatus}</strong></p>
          <p>Bill Status: <strong>{bill.billStatus}</strong></p>
        </div>

        <div className="bill-details-actions">
          <button
            type="button"
            className="secondary-button"
            onClick={() => navigate("/bills")}
          >
            Back to All Bills
          </button>


          <button
            type="button"
            className="primary-button"
            onClick={() => setShowPaymentForm(true)}
            disabled={Number(bill.pendingAmount) <= 0}
          >
            Record Payment
          </button>

          <button
            type="button"
            className="secondary-button"
            onClick={handleStartEditing}
            disabled={isEditing || bill.billStatus !== "ACTIVE"}
          >
            Edit Bill
          </button>

          <button
            type="button"
            className="primary-button"
            onClick={() => generateBillPdf(bill)}
          >
            Download PDF
          </button>
        </div>
      </section>


      {showPaymentForm && (
        <section className="content-card">
          <h2>Record Additional Payment</h2>
          <p className="muted">
            Pending balance: {formatCurrency(bill.pendingAmount)}
          </p>

          <form onSubmit={handleSavePayment}>
              {paymentFormError && (
                <p className="form-error" role="alert">
                  {paymentFormError}
                </p>
              )}
            <div className="form-group">
              <label htmlFor="additionalPaymentAmount">
                Amount Received (₹)
              </label>
              <input
                id="additionalPaymentAmount"
                type="number"
                min="0.01"
                max={bill.pendingAmount}
                step="0.01"
                placeholder="Enter amount received"
                value={paymentAmount}
                onChange={(event) => setPaymentAmount(event.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="additionalPaymentMethod">
                Payment Method
              </label>
              <select
                id="additionalPaymentMethod"
                value={paymentMethod}
                onChange={(event) => setPaymentMethod(event.target.value)}
                required
              >
                <option value="CASH">Cash</option>
                <option value="UPI">UPI</option>
                <option value="CARD">Card</option>
                <option value="BANK_TRANSFER">Bank Transfer</option>
                <option value="CHEQUE">Cheque</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="additionalPaymentReference">
                Payment Reference (Optional)
              </label>
              <input
                id="additionalPaymentReference"
                type="text"
                maxLength={100}
                placeholder="Transaction ID or receipt reference"
                value={paymentReference}
                onChange={(event) => setPaymentReference(event.target.value)}
              />
            </div>

            <div className="form-group">
              <label htmlFor="additionalPaymentNotes">
                Notes (Optional)
              </label>
              <textarea
                id="additionalPaymentNotes"
                maxLength={2000}
                rows={2}
                placeholder="Additional payment details"
                value={paymentNotes}
                onChange={(event) => setPaymentNotes(event.target.value)}
              />
            </div>

            <div className="bill-details-actions">
              <button
                type="button"
                className="secondary-button"
                onClick={() => setShowPaymentForm(false)}
              >
                Cancel
              </button>

              <button type="submit" className="primary-button">
                Save Payment
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="content-card payment-history-section">
        <h2>Payment History</h2>

        {paymentsLoading ? (
          <p>Loading payment history...</p>
        ) : paymentsError ? (
          <p className="form-error">{paymentsError}</p>
        ) : payments.length === 0 ? (
          <p className="muted">No payments recorded for this bill yet.</p>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Amount</th>
                  <th>Method</th>
                  <th>Reference</th>
                  <th>Notes</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((payment) => (
                  <tr key={payment.id}>
                    <td>
                      {payment.createdAt
                        ? new Date(payment.createdAt).toLocaleString("en-IN")
                        : "—"}
                    </td>
                    <td>
                      ₹{Number(payment.amount).toLocaleString("en-IN", {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      })}
                    </td>
                    <td>{payment.paymentMethod || "—"}</td>
                    <td>{payment.paymentReference || "—"}</td>
                    <td>{payment.notes || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}


function ItemCataloguePage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [showRateDialog, setShowRateDialog] = useState(false);
  const [ratePercentage, setRatePercentage] = useState("");
  const [rateError, setRateError] = useState("");
  const [rateLoading, setRateLoading] = useState(false);

  const [editingItemId, setEditingItemId] = useState(null);
  const [editFormData, setEditFormData] = useState({
    name: "",
    description: "",
    sku: "",
    unit: "",
    price: "",
  });

  const [formData, setFormData] = useState({
    name: "",
    description: "",
    sku: "",
    unit: "",
    price: "",
  });

  useEffect(() => {
    let cancelled = false;

    async function loadItems() {
      setLoading(true);
      setError("");

      try {
        const data = await apiRequest("/api/items");

        if (!cancelled) {
          setItems(Array.isArray(data) ? data : data.content ?? []);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || "Could not load catalogue items.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadItems();

    return () => {
      cancelled = true;
    };
  }, []);


  const formatCurrency = (amount) =>
    `₹${Number(amount ?? 0).toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;

  const filteredItems = items.filter((item) =>
    (item.name ?? "")
      .toLowerCase()
      .includes(searchQuery.trim().toLowerCase())
  );


  async function handleIncreaseRates() {
    setRateError("");

    const percentage = Number(ratePercentage);

    if (
      ratePercentage.trim() === "" ||
      !Number.isFinite(percentage) ||
      percentage <= 0 ||
      percentage > 1000
    ) {
      setRateError(
        "Enter a percentage greater than 0 and at most 1000."
      );
      return;
    }

    if (rateLoading) return;

    setRateLoading(true);

    try {
      const result = await apiRequest("/api/items/prices/increase", {
        method: "PATCH",
        body: JSON.stringify({ percentage }),
      });

      const updatedItems = await apiRequest("/api/items");

      setItems(
        Array.isArray(updatedItems)
          ? updatedItems
          : updatedItems.content ?? []
      );

      setShowRateDialog(false);
      setRatePercentage("");

      setSuccessMessage(
        `Rates increased by ${percentage}% for ${result.updatedItems} existing items.`
      );
    } catch (err) {
      setRateError(err.message || "Could not increase item rates.");
    } finally {
      setRateLoading(false);
    }
  }


  return (
    <div className="page-content">
      <p className="eyebrow">BILLING MANAGEMENT</p>
      <h1>Item Catalogue</h1>
      <p className="muted">
        Manage products, prices and active catalogue items.
      </p>

      <section className="content-card">


<div className="catalogue-toolbar">
  <button
    type="button"
    className="primary-button"
    onClick={() => {
      setShowCreateForm((current) => !current);
      setFormError("");
      setSuccessMessage("");
      setEditingItemId(null);
    }}
  >
    {showCreateForm ? "Cancel" : "+ Add Item"}
  </button>

  <div className="catalogue-search">
    <input
      type="search"
      aria-label="Search catalogue items"
      placeholder="Search items by name..."
      value={searchQuery}
      onChange={(event) => setSearchQuery(event.target.value)}
    />
    {searchQuery && (
      <button
        type="button"
        className="secondary-button"
        onClick={() => setSearchQuery("")}
      >
        Clear
      </button>
    )}
  </div>

  <button
    type="button"
    className="secondary-button"
    onClick={() => {
      setRatePercentage("");
      setRateError("");
      setShowRateDialog(true);
    }}
  >
    Increase Rate
  </button>
</div>

{showRateDialog && (
  <div className="rate-dialog-backdrop">
    <div
      className="rate-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="rate-dialog-title"
    >
      <h2 id="rate-dialog-title">Increase All Item Rates</h2>

      <p>
        Enter the percentage by which existing item prices should increase.
        This applies to active and inactive items in your shop.
      </p>

      <label>
        Increase percentage (%)
        <input
          type="number"
          min="0.01"
          max="1000"
          step="0.01"
          value={ratePercentage}
          onChange={(event) => setRatePercentage(event.target.value)}
          placeholder="e.g. 7"
          disabled={rateLoading}
        />
      </label>

      {ratePercentage.trim() !== "" &&
        Number.isFinite(Number(ratePercentage)) &&
        Number(ratePercentage) > 0 &&
        Number(ratePercentage) <= 1000 && (
          <p className="rate-preview">
            A ₹100 item will become{" "}
            {formatCurrency(100 * (1 + Number(ratePercentage) / 100))}.
          </p>
        )}

      <p className="muted">
        Existing saved bills will not change. New items added later will not
        be affected by this increase.
      </p>

      {rateError && (
        <p role="alert" className="form-error">
          {rateError}
        </p>
      )}

      <div className="bill-details-actions">
        <button
          type="button"
          className="secondary-button"
          disabled={rateLoading}
          onClick={() => {
            setShowRateDialog(false);
            setRateError("");
          }}
        >
          Cancel
        </button>

        <button
          type="button"
          className="primary-button"
          disabled={rateLoading}
          onClick={() => {
            const confirmed = window.confirm(
              `Increase all existing item rates by ${ratePercentage}%? This change affects your catalogue prices.`
            );

            if (confirmed) {
              handleIncreaseRates();
            }
          }}
        >
          {rateLoading ? "Updating Rates..." : "Confirm Increase"}
        </button>
      </div>
    </div>
  </div>
)}


{editingItemId && (
  <form
    className="item-create-form"
    onSubmit={async (event) => {
      event.preventDefault();
      setFormError("");
      setSuccessMessage("");

      const priceText = editFormData.price.trim();
      const priceValue = Number(priceText);

      if (!editFormData.name.trim()) {
        setFormError("Item name is required.");
        return;
      }

      if (!editFormData.unit.trim()) {
        setFormError("Unit is required.");
        return;
      }

      if (
        priceText === "" ||
        !Number.isFinite(priceValue) ||
        priceValue < 0 ||
        !/^\d{1,10}(\.\d{1,2})?$/.test(priceText)
      ) {
        setFormError(
          "Enter a valid price with up to 10 integer digits and 2 decimal places."
        );
        return;
      }

      setSaving(true);

      try {
        const updatedItem = await apiRequest(
          `/api/items/${editingItemId}`,
          {
            method: "PUT",
            body: JSON.stringify({
              name: editFormData.name.trim(),
              description: editFormData.description.trim() || null,
              sku: editFormData.sku.trim() || null,
              unit: editFormData.unit.trim(),
              price: priceValue,
            }),
          }
        );

        setItems((current) =>
          current.map((item) =>
            item.id === editingItemId ? updatedItem : item
          )
        );

        setEditingItemId(null);
        setSuccessMessage("Item updated successfully.");
      } catch (err) {
        setFormError(err.message || "Could not update item.");
      } finally {
        setSaving(false);
      }
    }}
  >
    <h2>Edit Item</h2>

    <div className="form-grid">
      <label>
        Item Name *
        <input
          type="text"
          maxLength={200}
          value={editFormData.name}
          onChange={(event) =>
            setEditFormData((current) => ({
              ...current,
              name: event.target.value,
            }))
          }
          required
        />
      </label>

      <label>
        SKU
        <input
          type="text"
          maxLength={100}
          value={editFormData.sku}
          onChange={(event) =>
            setEditFormData((current) => ({
              ...current,
              sku: event.target.value,
            }))
          }
        />
      </label>

      <label>
        Unit *
        <input
          type="text"
          maxLength={30}
          value={editFormData.unit}
          onChange={(event) =>
            setEditFormData((current) => ({
              ...current,
              unit: event.target.value,
            }))
          }
          required
        />
      </label>

      <label>
        Price (₹) *
        <input
          type="number"
          min="0"
          max="9999999999.99"
          step="0.01"
          value={editFormData.price}
          onChange={(event) =>
            setEditFormData((current) => ({
              ...current,
              price: event.target.value,
            }))
          }
          required
        />
      </label>

      <label className="form-full-width">
        Description
        <textarea
          maxLength={2000}
          rows={3}
          value={editFormData.description}
          onChange={(event) =>
            setEditFormData((current) => ({
              ...current,
              description: event.target.value,
            }))
          }
        />
      </label>
    </div>

    {formError && (
      <p role="alert" className="form-error">
        {formError}
      </p>
    )}

    <div className="bill-details-actions">
      <button
        type="button"
        className="secondary-button"
        onClick={() => {
          setEditingItemId(null);
          setFormError("");
        }}
        disabled={saving}
      >
        Cancel
      </button>

      <button
        type="submit"
        className="primary-button"
        disabled={saving}
      >
        {saving ? "Saving..." : "Save Item"}
      </button>
    </div>
  </form>
)}

{successMessage && (
  <p role="status" className="success-message">
    {successMessage}
  </p>
)}

{showCreateForm && (
  <form
    className="item-create-form"
    onSubmit={async (event) => {
      event.preventDefault();
      setFormError("");
      setSuccessMessage("");

      const priceValue = Number(formData.price);

      if (!formData.name.trim()) {
        setFormError("Item name is required.");
        return;
      }

      if (!formData.unit.trim()) {
        setFormError("Unit is required.");
        return;
      }

      if (
        formData.price.trim() === "" ||
        !Number.isFinite(priceValue) ||
        priceValue < 0 ||
        !/^\d{1,10}(\.\d{1,2})?$/.test(formData.price.trim())
      ) {
        setFormError(
          "Enter a valid price with up to 10 integer digits and 2 decimal places."
        );
        return;
      }

      setSaving(true);

      try {
        const createdItem = await apiRequest("/api/items", {
          method: "POST",
          body: JSON.stringify({
            name: formData.name.trim(),
            description: formData.description.trim() || null,
            sku: formData.sku.trim() || null,
            unit: formData.unit.trim(),
            price: priceValue,
          }),
        });

        setItems((current) => [
          createdItem,
          ...current.filter((item) => item.id !== createdItem.id),
        ]);

        setFormData({
          name: "",
          description: "",
          sku: "",
          unit: "",
          price: "",
        });

        setShowCreateForm(false);
        setSuccessMessage("Item created successfully.");
      } catch (err) {
        setFormError(err.message || "Could not create item.");
      } finally {
        setSaving(false);
      }
    }}
  >
    <div className="form-grid">
      <label>
        Item Name *
        <input
          type="text"
          maxLength={200}
          value={formData.name}
          onChange={(event) =>
            setFormData((current) => ({
              ...current,
              name: event.target.value,
            }))
          }
          required
        />
      </label>

      <label>
        SKU
        <input
          type="text"
          maxLength={100}
          value={formData.sku}
          onChange={(event) =>
            setFormData((current) => ({
              ...current,
              sku: event.target.value,
            }))
          }
        />
      </label>

      <label>
        Unit *
        <input
          type="text"
          maxLength={30}
          placeholder="e.g. piece, box, packet"
          value={formData.unit}
          onChange={(event) =>
            setFormData((current) => ({
              ...current,
              unit: event.target.value,
            }))
          }
          required
        />
      </label>

      <label>
        Price (₹) *
        <input
          type="number"
          min="0"
          max="9999999999.99"
          step="0.01"
          value={formData.price}
          onChange={(event) =>
            setFormData((current) => ({
              ...current,
              price: event.target.value,
            }))
          }
          required
        />
      </label>

      <label className="form-full-width">
        Description
        <textarea
          maxLength={2000}
          rows={3}
          value={formData.description}
          onChange={(event) =>
            setFormData((current) => ({
              ...current,
              description: event.target.value,
            }))
          }
        />
      </label>
    </div>

    {formError && (
      <p role="alert" className="form-error">
        {formError}
      </p>
    )}

    <button
      type="submit"
      className="primary-button"
      disabled={saving}
    >
      {saving ? "Saving Item..." : "Save Item"}
    </button>
  </form>
)}

        {loading ? (
          <p>Loading catalogue items...</p>
        ) : error ? (
          <p role="alert">{error}</p>
        ) : items.length === 0 ? (
          <p>No catalogue items found.</p>
        ) : filteredItems.length === 0 ? (
          <p>No items match "{searchQuery}".</p>
        ) : (
          <div className="bill-items-table-wrapper">
            <table className="bill-items-table">
              <thead>
                <tr>
                  <th>Item Name</th>
                  <th>SKU</th>
                  <th>Unit</th>
                  <th>Price</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredItems.map((item) => (
                  <tr key={item.id}>
                    <td>{item.name}</td>
                    <td>{item.sku || "—"}</td>
                    <td>{item.unit || "—"}</td>
                    <td>{formatCurrency(item.price)}</td>
                    <td>{item.active ? "Active" : "Inactive"}</td>
                    <td>
                      <button
                        type="button"
                        className="secondary-button"
                        onClick={() => {
                          setEditingItemId(item.id);
                          setEditFormData({
                            name: item.name ?? "",
                            description: item.description ?? "",
                            sku: item.sku ?? "",
                            unit: item.unit ?? "",
                            price: String(item.price ?? ""),
                          });
                          setFormError("");
                          setSuccessMessage("");
                          setShowCreateForm(false);
                        }}
                      >
                        Edit
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function CreateBillPage() {
  const [customerName, setCustomerName] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [catalogueItems, setCatalogueItems] = useState([]);
  const [items, setItems] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [filteredItems, setFilteredItems] = useState([]);
  const [highlightedIndex, setHighlightedIndex] = useState(0);
  const [pendingItem, setPendingItem] = useState(null);
  const [quantityText, setQuantityText] = useState("");
  const [formError, setFormError] = useState("");
  const [catalogueLoading, setCatalogueLoading] = useState(true);
  const [catalogueError, setCatalogueError] = useState("");
  const [saving, setSaving] = useState(false);
  const [savedBill, setSavedBill] = useState(null);
  const [paymentChoice, setPaymentChoice] = useState("later");
  const [initialPaymentAmount, setInitialPaymentAmount] = useState("");
  const [initialPaymentMethod, setInitialPaymentMethod] = useState("CASH");
  const [paymentReference, setPaymentReference] = useState("");
  const [paymentNotes, setPaymentNotes] = useState("");

  const searchRef = useRef(null);
  const quantityRef = useRef(null);
  const dropdownRef = useRef(null);
  const customerNameRef = useRef(null);

  useEffect(() => {
    let cancelled = false;

    async function loadCatalogue() {
      setCatalogueLoading(true);
      setCatalogueError("");

      try {
        const data = await apiRequest("/api/items");

        if (!Array.isArray(data)) {
          throw new Error("Unexpected catalogue response.");
        }

        if (!cancelled) {
          setCatalogueItems(data.filter((item) => item.active));
        }
      } catch (error) {
        if (!cancelled) {
          setCatalogueError(error.message || "Could not load catalogue items.");
        }
      } finally {
        if (!cancelled) {
          setCatalogueLoading(false);
        }
      }
    }

    loadCatalogue();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const query = searchText.trim().toLowerCase();

    if (!query || pendingItem) {
      setFilteredItems([]);
      setHighlightedIndex(0);
      return;
    }

    const matches = catalogueItems
      .filter((item) => {
        const name = (item.name || "").toLowerCase();
        const sku = (item.sku || "").toLowerCase();

        return name.includes(query) || sku.includes(query);
      })
      .filter(
        (item) => !items.some((addedItem) => addedItem.id === item.id)
      )
      .slice(0, 12);

    setFilteredItems(matches);
    setHighlightedIndex(0);
  }, [searchText, catalogueItems, items, pendingItem]);

  useEffect(() => {
    if (pendingItem && quantityRef.current) {
      quantityRef.current.focus();
    }
  }, [pendingItem]);

  useEffect(() => {
    const handleOutsideClick = (event) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target) &&
        event.target !== searchRef.current
      ) {
        setFilteredItems([]);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);

    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
    };
  }, []);

  const grandTotal = items.reduce(
    (total, item) => total + item.price * item.quantity,
    0
  );

  function selectItem(item) {
    setPendingItem(item);
    setQuantityText("");
    setSearchText("");
    setFilteredItems([]);
    setFormError("");
  }

  function handleSearchKeyDown(event) {
    if (event.key === "ArrowDown" && filteredItems.length > 0) {
      event.preventDefault();
      setHighlightedIndex((index) =>
        Math.min(index + 1, filteredItems.length - 1)
      );
      return;
    }

    if (event.key === "ArrowUp" && filteredItems.length > 0) {
      event.preventDefault();
      setHighlightedIndex((index) => Math.max(index - 1, 0));
      return;
    }

    if (event.key === "Enter") {
      event.preventDefault();

      if (filteredItems.length > 0) {
        selectItem(filteredItems[highlightedIndex]);
      }

      return;
    }

    if (event.key === "Escape") {
      setFilteredItems([]);
    }
  }

  function handleQuantityKeyDown(event) {
    if (event.key === "Enter") {
      event.preventDefault();
      addPendingItem();
    }

    if (event.key === "Escape") {
      setPendingItem(null);
      setQuantityText("");
      setTimeout(() => searchRef.current?.focus(), 0);
    }
  }

  function addPendingItem() {
    if (!pendingItem) {
      return;
    }

    const quantity = Number(quantityText);

    if (!Number.isSafeInteger(quantity) || quantity < 1) {
      setFormError("Enter a whole-number quantity of at least 1.");
      quantityRef.current?.focus();
      return;
    }

    setItems((currentItems) => [
      ...currentItems,
      {
        id: pendingItem.id,
        name: pendingItem.name,
        price: Number(pendingItem.price),
        unit: pendingItem.unit,
        quantity,
      },
    ]);

    setPendingItem(null);
    setQuantityText("");
    setSearchText("");
    setFilteredItems([]);
    setFormError("");

    setTimeout(() => searchRef.current?.focus(), 0);
  }

  function handleRemoveItem(itemId) {
    setItems((currentItems) =>
      currentItems.filter((item) => item.id !== itemId)
    );
  }

  async function handleSaveBill() {
    setFormError("");

    if (!customerName.trim()) {
      setFormError("Customer name is required.");
      customerNameRef.current?.focus();
      return;
    }

    if (customerName.trim().length > 150) {
      setFormError("Customer name cannot exceed 150 characters.");
      customerNameRef.current?.focus();
      return;
    }

    if (!/^[6-9]\d{9}$/.test(customerPhone)) {
      setFormError("Enter a valid 10-digit Indian mobile number.");
      return;
    }

    if (pendingItem) {
      setFormError("Finish entering the selected item's quantity first.");
      quantityRef.current?.focus();
      return;
    }

    if (items.length === 0) {
      setFormError("Add at least one item to the bill.");
      searchRef.current?.focus();
      return;
    }

    if (paymentChoice === "now") {
      const amount = Number(initialPaymentAmount);

      if (
        !initialPaymentAmount.trim() ||
        !Number.isFinite(amount) ||
        amount <= 0
      ) {
        setFormError("Enter an initial payment amount greater than zero.");
        return;
      }

      if (!/^\d{1,10}(\.\d{1,2})?$/.test(initialPaymentAmount)) {
        setFormError("Payment amount can have at most 2 decimal places.");
        return;
      }

      if (amount > grandTotal) {
        setFormError("Initial payment cannot exceed the bill total.");
        return;
      }
    }

    const invalidItem = items.some(
      (item) =>
        !Number.isSafeInteger(item.quantity) ||
        item.quantity < 1
    );

    if (invalidItem) {
      setFormError("Each item must have a whole-number quantity of at least 1.");
      return;
    }

    setSaving(true);

    try {

const requestBody = {
  customerName: customerName.trim(),
  customerPhone,
  items: items.map((item) => ({
    itemId: item.id,
    quantity: item.quantity,
  })),
  initialPayment:
    paymentChoice === "now"
      ? {
          amount: initialPaymentAmount,
          paymentMethod: initialPaymentMethod,
          paymentReference: paymentReference.trim() || null,
          notes: paymentNotes.trim() || null,
        }
      : null,
};

      const response = await apiRequest("/api/bills", {
        method: "POST",
        body: JSON.stringify(requestBody),
      });

      setSavedBill(response);
      setFormError("");
    } catch (error) {
      setFormError(error.message || "Could not save the bill. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  async function handleIncreaseRates() {
    setRateError("");

    const percentage = Number(ratePercentage);

    if (
      ratePercentage.trim() === "" ||
      !Number.isFinite(percentage) ||
      percentage <= 0 ||
      percentage > 1000
    ) {
      setRateError("Enter a percentage greater than 0 and at most 1000.");
      return;
    }

    if (rateLoading) return;

    setRateLoading(true);

    try {
      const result = await apiRequest("/api/items/prices/increase", {
        method: "PATCH",
        body: JSON.stringify({ percentage }),
      });

      const updatedItems = await apiRequest("/api/items");
      setItems(
        Array.isArray(updatedItems)
          ? updatedItems
          : updatedItems.content ?? []
      );

      setShowRateDialog(false);
      setRatePercentage("");
      setSuccessMessage(
        `Rates increased by ${percentage}% for ${result.updatedItems} existing items.`
      );
    } catch (err) {
      setRateError(err.message || "Could not increase item rates.");
    } finally {
      setRateLoading(false);
    }
  }


  return (
    <div className="page-content">
      <div className="page-heading">
        <div>
          <h1>Create New Bill</h1>
          <p>Enter customer details and add items to the bill.</p>
        </div>
      </div>

      <div className="content-card">
        <h2>Customer Details</h2>

        <div className="form-grid">
          <div className="form-group">
            <label htmlFor="customerName">Customer Name</label>
            <input
              ref={customerNameRef}
              id="customerName"
              type="text"
              maxLength={150}
              placeholder="Enter customer name"
              value={customerName}
              onChange={(event) => setCustomerName(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  searchRef.current?.focus();
                }
              }}
            />
          </div>

          <div className="form-group">
            <label htmlFor="customerPhone">Mobile Number</label>
            <input
              id="customerPhone"
              type="tel"
              inputMode="numeric"
              maxLength={10}
              placeholder="10-digit mobile number"
              value={customerPhone}
              onChange={(event) =>
                setCustomerPhone(
                  event.target.value.replace(/\D/g, "").slice(0, 10)
                )
              }
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  searchRef.current?.focus();
                }
              }}
            />
          </div>
        </div>
      </div>

      <div className="content-card">
        <h2>Add Items</h2>

        {catalogueLoading ? (
          <p>Loading catalogue items...</p>
        ) : catalogueError ? (
          <p className="form-error">{catalogueError}</p>
        ) : catalogueItems.length === 0 ? (
          <p>No active catalogue items found. Add items to your catalogue first.</p>
        ) : (
          <>
            <div className="form-group item-search-group">
              <label htmlFor="itemSearch">Search catalogue</label>
              <input
                ref={searchRef}
                id="itemSearch"
                type="text"
                autoComplete="off"
                placeholder="Type an item name or part of it..."
                value={searchText}
                onChange={(event) => {
                  setSearchText(event.target.value);
                  setFormError("");
                }}
                onFocus={() => {
                  if (searchText.trim()) {
                    setFilteredItems(
                      catalogueItems
                        .filter((item) => {
                          const query = searchText.trim().toLowerCase();
                          return (
                            item.name?.toLowerCase().includes(query) ||
                            item.sku?.toLowerCase().includes(query)
                          );
                        })
                        .filter(
                          (item) =>
                            !items.some((added) => added.id === item.id)
                        )
                        .slice(0, 12)
                    );
                  }
                }}
                onKeyDown={handleSearchKeyDown}
                disabled={Boolean(pendingItem)}
              />

              {filteredItems.length > 0 && (
                <div
                  className="item-search-results"
                  ref={dropdownRef}
                  role="listbox"
                >
                  {filteredItems.map((item, index) => (
                    <button
                      type="button"
                      key={item.id}
                      role="option"
                      aria-selected={index === highlightedIndex}
                      className={`item-search-option ${
                        index === highlightedIndex ? "highlighted" : ""
                      }`}
                      onMouseEnter={() => setHighlightedIndex(index)}
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => selectItem(item)}
                    >
                      <span className="item-option-name">{item.name}</span>
                      <span className="item-option-meta">
                        ₹{Number(item.price).toLocaleString("en-IN")} /{" "}
                        {item.unit}
                      </span>
                    </button>
                  ))}
                </div>
              )}

              {searchText.trim() && filteredItems.length === 0 && !pendingItem && (
                <p className="search-hint">No matching items found.</p>
              )}
            </div>

            {pendingItem && (
              <div className="quantity-entry">
                <div className="quantity-item-details">
                  <strong>{pendingItem.name}</strong>
                  <span>
                    ₹{Number(pendingItem.price).toLocaleString("en-IN")} /{" "}
                    {pendingItem.unit}
                  </span>
                </div>

                <div className="form-group quantity-input-group">
                  <label htmlFor="itemQuantity">Quantity</label>
                  <input
                    ref={quantityRef}
                    id="itemQuantity"
                    type="number"
                    inputMode="numeric"
                    min="1"
                    step="1"
                    placeholder="Enter quantity"
                    value={quantityText}
                    onChange={(event) => {
                      const value = event.target.value;
                      if (/^\d*$/.test(value)) {
                        setQuantityText(value);
                        setFormError("");
                      }
                    }}
                    onKeyDown={handleQuantityKeyDown}
                  />
                </div>

                <button
                  type="button"
                  className="primary-button"
                  onClick={addPendingItem}
                >
                  Add to Bill
                </button>

                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => {
                    setPendingItem(null);
                    setQuantityText("");
                    setTimeout(() => searchRef.current?.focus(), 0);
                  }}
                >
                  Cancel
                </button>
              </div>
            )}
          </>
        )}

        {items.length > 0 && (
          <div className="table-wrapper bill-items-table-wrapper">
            <table className="data-table bill-items-table">
              <thead>
                <tr>
                  <th>Item</th>
                  <th>Price</th>
                  <th>Quantity</th>
                  <th>Subtotal</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td>{item.name}</td>
                    <td>
                      ₹
                      {item.price.toLocaleString("en-IN", {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      })}
                    </td>
                    <td>{item.quantity}</td>
                    <td>
                      ₹
                      {(item.price * item.quantity).toLocaleString("en-IN", {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      })}
                    </td>
                    <td>
                      <button
                        type="button"
                        className="secondary-button"
                        onClick={() => handleRemoveItem(item.id)}
                      >
                        Remove
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="content-card bill-total-card">
        <h2>Grand Total</h2>
        <p className="bill-grand-total">
          ₹
          {grandTotal.toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })}
        </p>

        {formError && <p className="form-error">{formError}</p>}


<div className="initial-payment-section">
  <h3>Payment</h3>
  <p className="muted">
    Will the customer make a payment now or pay later?
  </p>

  <div className="payment-choice-group">
    <label className="payment-choice">
      <input
        type="radio"
        name="paymentChoice"
        value="later"
        checked={paymentChoice === "later"}
        onChange={() => setPaymentChoice("later")}
      />
      <span>Pay Later</span>
    </label>

    <label className="payment-choice">
      <input
        type="radio"
        name="paymentChoice"
        value="now"
        checked={paymentChoice === "now"}
        onChange={() => setPaymentChoice("now")}
      />
      <span>Pay Now</span>
    </label>
  </div>

  {paymentChoice === "now" && (
    <div className="initial-payment-fields">
      <div className="form-group">
        <label htmlFor="initialPaymentAmount">Amount Received (₹)</label>
        <input
          id="initialPaymentAmount"
          type="number"
          min="0.01"
          max={grandTotal}
          step="0.01"
          placeholder="Enter amount received"
          value={initialPaymentAmount}
          onChange={(event) => setInitialPaymentAmount(event.target.value)}
        />
        <p className="muted">
          Grand total: ₹
          {grandTotal.toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })}
        </p>
      </div>

      <div className="form-group">
        <label htmlFor="initialPaymentMethod">Payment Method</label>
        <select
          id="initialPaymentMethod"
          value={initialPaymentMethod}
          onChange={(event) => setInitialPaymentMethod(event.target.value)}
        >
          <option value="CASH">Cash</option>
          <option value="UPI">UPI</option>
          <option value="CARD">Card</option>
          <option value="BANK_TRANSFER">Bank Transfer</option>
          <option value="CHEQUE">Cheque</option>
          <option value="OTHER">Other</option>
        </select>
      </div>

      <div className="form-group">
        <label htmlFor="paymentReference">Payment Reference (Optional)</label>
        <input
          id="paymentReference"
          type="text"
          maxLength={100}
          value={paymentReference}
          onChange={(event) => setPaymentReference(event.target.value)}
          placeholder="Transaction ID or receipt reference"
        />
      </div>

      <div className="form-group">
        <label htmlFor="paymentNotes">Payment Notes (Optional)</label>
        <textarea
          id="paymentNotes"
          maxLength={2000}
          value={paymentNotes}
          onChange={(event) => setPaymentNotes(event.target.value)}
          placeholder="Additional payment details"
          rows={2}
        />
      </div>
    </div>
  )}
</div>


        {savedBill && (
          <div className="saved-bill-message">
            <h3>Bill saved successfully!</h3>

            <p>
              Bill Number: <strong>{savedBill.billNumber}</strong>
            </p>

            <p>
              Total: ₹
              {Number(savedBill.totalAmount).toLocaleString("en-IN", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </p>

            <button
              type="button"
              className="primary-button"
              onClick={() => {
                try {
                  generateBillPdf(savedBill);
                } catch (error) {
                  console.error("PDF generation failed:", error);
                  setFormError("Could not generate the PDF. Please try again.");
                }
              }}
            >
              Download PDF
            </button>
          </div>
        )}




        <div className="bill-actions">
          <button
            type="button"
            className="primary-button"
            onClick={handleSaveBill}
            disabled={saving || Boolean(savedBill)}
          >
            {saving ? "Saving Bill..." : savedBill ? "Bill Saved" : "Save Bill"}
          </button>


        </div>
      </div>
    </div>
  );
}


function AppLayout() {
    const navigate = useNavigate();

      function handleLogout() {
        localStorage.removeItem("token");
        localStorage.removeItem("user");

        window.dispatchEvent(new Event("auth-change"));

        navigate("/login", { replace: true });
      }

return ( <div className="app-shell"> <aside className="sidebar"> <div className="brand"> <div className="brand-mark">B</div> <div> <h2>Jay Ganesh</h2> <p>Billing workspace</p> </div> </div>


    <div className="nav-label">WORKSPACE</div>
    <nav className="navigation">
      <NavLink to="/dashboard" icon="▦" label="Dashboard" />
      <NavLink to="/bills" icon="▤" label="All Bills" />
      <NavLink to="/bills/new" icon="＋" label="Create Bill" />
      <NavLink to="/payments" icon="₹" label="Payments" />
      <NavLink to="/items" icon="▧" label="Item Catalogue" />
    </nav>

    <div className="sidebar-footer">
      <div className="avatar">A</div>
      <div>
        <strong>Shop Admin</strong>
        <p>Billing account</p>
      </div>
    </div>
  </aside>

  <main className="main-area">
    <header className="topbar">
      <div className="breadcrumb">Workspace <span>/</span> Billing</div>
      <div className="topbar-right">
        <span className="status-dot" />
        <span>Local demo</span>

        <button
          type="button"
          className="primary-button"
          onClick={handleLogout}
        >
          Logout
        </button>
      </div>
    </header>

    <Routes>
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/bills" element={<AllBillsPage />} />
      <Route path="/bills/:billId" element={<BillDetailsPage />} />
      <Route
        path="/bills/new"
        element={<CreateBillPage />}
      />
      <Route
        path="/payments"
        element={
          <PlaceholderPage
            title="Payments"
            description="Review payments and outstanding balances."
          />
        }
      />
     <Route path="/items" element={<ItemCataloguePage />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  </main>
</div>


);
}

function NavLink({ to, icon, label }) {
  return (
    <RouterNavLink
      to={to}
      end
      className={({ isActive }) =>
        `nav-link ${isActive ? "active" : ""}`
      }
    >
      <span className="nav-icon">{icon}</span>
      <span>{label}</span>
    </RouterNavLink>
  );
}

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(
    Boolean(localStorage.getItem("token"))
  );

  useEffect(() => {
    function syncAuthentication() {
      setIsAuthenticated(Boolean(localStorage.getItem("token")));
    }

    window.addEventListener("auth-change", syncAuthentication);

    return () => {
      window.removeEventListener("auth-change", syncAuthentication);
    };
  }, []);

  return (
    <Routes>
      <Route
        path="/login"
        element={
          isAuthenticated
            ? <Navigate to="/dashboard" replace />
            : <Login />
        }
      />

      <Route
        path="/*"
        element={
          isAuthenticated
            ? <AppLayout />
            : <Navigate to="/login" replace />
        }
      />
    </Routes>
  );
}