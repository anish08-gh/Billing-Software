
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";
import { apiRequest } from "../services/api";

const C = {
  navy: [11, 18, 69],
  orange: [255, 107, 15],
  teal: [14, 148, 136],
  crimson: [224, 0, 77],
  lightBlue: [223, 243, 248],
  lavender: [241, 237, 246],
  bg: [255, 253, 249],
  text: [17, 24, 39],
  grid: [217, 214, 224],
  gold: [242, 182, 50],
  white: [255, 255, 255],
  muted: [105, 112, 125],
  green: [20, 126, 85],
  paleGreen: [233, 247, 239],
  paleOrange: [255, 243, 230],
  paleRed: [255, 237, 242],
};

const money = (amount) =>
  `Rs. ${Number(amount || 0).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;

const formatDate = (value) => {
  if (!value) return "—";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return date.toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

function textValue(value, fallback = "—") {
  if (value === null || value === undefined || value === "") {
    return fallback;
  }

  return String(value);
}

// Draw a small decorative firework using vector lines.
function drawFirework(pdf, cx, cy, radius, color) {
  pdf.setDrawColor(...color);
  pdf.setFillColor(...color);
  pdf.setLineWidth(0.35);

  for (let i = 0; i < 12; i += 1) {
    const angle = (Math.PI * 2 * i) / 12;
    const innerRadius = radius * 0.25;
    const outerRadius = i % 2 === 0 ? radius : radius * 0.68;

    pdf.line(
      cx + Math.cos(angle) * innerRadius,
      cy + Math.sin(angle) * innerRadius,
      cx + Math.cos(angle) * outerRadius,
      cy + Math.sin(angle) * outerRadius
    );

    if (i % 2 === 0) {
      pdf.circle(
        cx + Math.cos(angle) * (radius + 0.7),
        cy + Math.sin(angle) * (radius + 0.7),
        0.6,
        "F"
      );
    }
  }

  pdf.circle(cx, cy, 0.7, "F");
}

// A simple festive diya illustration.
function drawDiya(pdf, cx, cy, scale = 1) {
  pdf.setFillColor(255, 183, 59);
  pdf.ellipse(cx, cy, 4.8 * scale, 1.6 * scale, "F");

  pdf.setFillColor(194, 87, 26);
  pdf.ellipse(cx, cy + 1.1 * scale, 4.8 * scale, 1.8 * scale, "F");

  pdf.setDrawColor(...C.gold);
  pdf.setLineWidth(0.4);
  pdf.line(
    cx - 3.5 * scale,
    cy + 1.1 * scale,
    cx + 3.5 * scale,
    cy + 1.1 * scale
  );

  // Flame.
  pdf.setFillColor(255, 107, 15);
  pdf.triangle(
    cx,
    cy - 5.5 * scale,
    cx - 1.7 * scale,
    cy - 0.3 * scale,
    cx + 1.7 * scale,
    cy - 0.3 * scale,
    "F"
  );

  pdf.setFillColor(255, 239, 164);
  pdf.triangle(
    cx,
    cy - 3.4 * scale,
    cx - 0.7 * scale,
    cy - 0.3 * scale,
    cx + 0.7 * scale,
    cy - 0.3 * scale,
    "F"
  );
}

function drawCard(pdf, x, y, width, height, color, title, icon) {
  pdf.setFillColor(...C.white);
  pdf.setDrawColor(...C.grid);
  pdf.setLineWidth(0.3);
  pdf.roundedRect(x, y, width, height, 2.5, 2.5, "FD");

  pdf.setFillColor(...color);
  pdf.roundedRect(x, y, width, 8, 2.5, 2.5, "F");
  pdf.rect(x, y + 4, width, 4, "F");

  pdf.setFillColor(...C.white);
  pdf.circle(x + 5.5, y + 4, 2.4, "F");

  pdf.setTextColor(...color);
  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(7);
  pdf.text(icon, x + 5.5, y + 5.2, { align: "center" });

  pdf.setTextColor(...C.white);
  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(8);
  pdf.text(title, x + 10, y + 5.2);
}

function writeWrapped(pdf, value, x, y, width, options = {}) {
  const lines = pdf.splitTextToSize(String(value), width);
  pdf.text(lines, x, y, options);
  return lines.length;
}

function drawHeader(pdf, shopProfile, bill, logo, pageWidth) {
  const margin = 15;
  const headerHeight = 29;

  // Page background.
  pdf.setFillColor(...C.bg);
  pdf.rect(0, 0, pageWidth, pdf.internal.pageSize.getHeight(), "F");

  // Decorative top strip.
  pdf.setFillColor(...C.orange);
  pdf.rect(0, 0, pageWidth, 2.2, "F");

  // Festive shop banner.
  pdf.setFillColor(...C.navy);
  pdf.roundedRect(
    8,
    7,
    pageWidth - 16,
    headerHeight,
    2.5,
    2.5,
    "F"
  );

  pdf.setDrawColor(...C.orange);
  pdf.setLineWidth(0.45);
  pdf.roundedRect(
    8,
    7,
    pageWidth - 16,
    headerHeight,
    2.5,
    2.5,
    "S"
  );

  drawFirework(pdf, 18, 17, 4, C.gold);
  drawFirework(pdf, pageWidth - 18, 17, 4, C.crimson);

  if (logo) {
    try {
      pdf.addImage(logo, "PNG", 25, 11, 21, 21, undefined, "FAST");
    } catch {
      // The invoice remains usable without a logo.
    }
  }


const shopName = textValue(shopProfile?.name, "SHREE DATTA FATAKA MART")
  .trim()
  .slice(0, 55);

// Hard-coded details for the PDF only.
const shopOwner = "Mayur Sawant";
const shopPhone = textValue(shopProfile?.phone, "");
const secondShopPhone = "9809099797";


  const centerX = pageWidth / 2 + (logo ? 5 : 0);

  pdf.setTextColor(...C.white);
  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(24);

  const fittedName = pdf.splitTextToSize(shopName, 110);
  pdf.text(fittedName.slice(0, 1), centerX, 16, {
    align: "center",
  });

  pdf.setTextColor(...C.gold);
  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(12);
  pdf.text("FIREWORKS  •  DIWALI SPECIAL", centerX, 22, {
    align: "center",
  });


pdf.setTextColor(...C.white);
pdf.setFont("helvetica", "normal");
pdf.setFontSize(9);

pdf.text(`Owner: ${shopOwner}`, centerX, 26, {
  align: "center",
});

pdf.setFontSize(9);

const contactNumbers = [shopPhone, secondShopPhone]
  .filter((phone) => phone && phone.trim() !== "")
  .join(" / ");

pdf.text(`Contact: ${contactNumbers}`, centerX, 30, {
  align: "center",
});


  // Invoice and customer information cards.
  const gap = 5;
  const cardY = 43;
  const cardWidth = (pageWidth - margin * 2 - gap) / 2;
  const cardHeight = 28;

  drawCard(
    pdf,
    margin,
    cardY,
    cardWidth,
    cardHeight,
    C.orange,
    "BILL DETAILS",
    "B"
  );

  drawCard(
    pdf,
    margin + cardWidth + gap,
    cardY,
    cardWidth,
    cardHeight,
    C.teal,
    "CUSTOMER DETAILS",
    "C"
  );

  pdf.setTextColor(...C.text);
  pdf.setFont("helvetica", "normal");
  pdf.setFontSize(8);

  pdf.text(
    `Bill No: ${textValue(bill.billNumber)}`,
    margin + 4,
    cardY + 15
  );

  pdf.text(
    `Date: ${formatDate(bill.createdAt)}`,
    margin + 4,
    cardY + 23
  );

  const customerX = margin + cardWidth + gap + 4;
  const customerWidth = cardWidth - 8;

  pdf.setFont("helvetica", "bold");
  const customerName = textValue(bill.customerName);
  const nameLines = pdf.splitTextToSize(customerName, customerWidth);

  pdf.text(nameLines.slice(0, 1), customerX, cardY + 15);

  pdf.setFont("helvetica", "normal");
  pdf.setFontSize(8);

  pdf.text(
    `Mobile: ${textValue(bill.customerPhone)}`,
    customerX,
    cardY + 23
  );

  // Small accent underneath the information cards.
  pdf.setFillColor(...C.gold);
  pdf.roundedRect(margin, 81, pageWidth - margin * 2, 1.2, 0.5, 0.5, "F");

  return 88;
}

function drawFooter(pdf, pageWidth, pageHeight, pageNumber, pageCount) {
  const margin = 15;

  // Keep footer illustrations and text in a reserved bottom area.
  pdf.setFillColor(...C.navy);
  pdf.rect(0, pageHeight - 17, pageWidth, 17, "F");

  pdf.setDrawColor(...C.orange);
  pdf.setLineWidth(0.6);
  pdf.line(0, pageHeight - 17, pageWidth, pageHeight - 17);

  drawFirework(pdf, 13, pageHeight - 8.5, 3, C.gold);
  drawFirework(pdf, pageWidth - 13, pageHeight - 8.5, 3, C.crimson);

  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(8);
  pdf.setTextColor(...C.gold);
  pdf.text("HAPPY DIWALI!  VISIT AGAIN!", pageWidth / 2, pageHeight - 9, {
    align: "center",
  });

  pdf.setFont("helvetica", "normal");
  pdf.setFontSize(6.5);
  pdf.setTextColor(...C.white);
  pdf.text(
    `Page ${pageNumber} of ${pageCount}`,
    pageWidth - margin,
    pageHeight - 3,
    { align: "right" }
  );
}

export async function generateBillPdf(bill) {
  if (!bill || typeof bill !== "object") {
    throw new Error("A valid bill is required to generate the PDF.");
  }

  const shopProfile = await apiRequest("/api/shop/profile");

  const pdf = new jsPDF({
    orientation: "portrait",
    unit: "mm",
    format: "a4",
    compress: true,
  });

  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();
  const margin = 15;
  const rightEdge = pageWidth - margin;
  const contentWidth = pageWidth - margin * 2;
  const footerReserve = 23;

  // Load the shop logo. A missing logo must not prevent bill generation.
  const loadLogo = () =>
    new Promise((resolve) => {
      const image = new Image();

      image.onload = () => resolve(image);
      image.onerror = () => resolve(null);
      image.src = "/images/logo.png";
    });

  const logo = await loadLogo();

  // Preserve the existing backend bill structure.
  const sortedItems = [...(bill.items || [])].sort(
    (a, b) => (a.lineNumber ?? 0) - (b.lineNumber ?? 0)
  );

  const itemsTotal = sortedItems.reduce(
    (sum, item) => sum + Number(item.totalAmount ?? 0),
    0
  );

  const subtotal = Number(bill.subtotal ?? itemsTotal);
  const total = Number(bill.totalAmount ?? subtotal);
  const amountPaid = Number(bill.amountPaid ?? 0);
  const pendingAmount = Number(
    bill.pendingAmount ?? Math.max(0, total - amountPaid)
  );

  // Normalize item descriptions without changing the source data.
  const tableRows = sortedItems.map((item, index) => {
    const itemName = textValue(item.itemName, "Item");
    const brand = item.brand ? String(item.brand) : "";

    return [
      String(index + 1),
      brand ? `${itemName}\n${brand}` : itemName,
      textValue(item.unit),
      Number(item.quantity || 0).toLocaleString("en-IN"),
      money(item.unitPrice),
      money(item.totalAmount),
    ];
  });

  // First-page background and branding.
  let tableStartY = drawHeader(
    pdf,
    shopProfile,
    bill,
    logo,
    pageWidth
  );

  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(8);
  pdf.setTextColor(...C.navy);
  pdf.text("ITEMS PURCHASED", margin, tableStartY - 3);

  autoTable(pdf, {
    startY: tableStartY,
    head: [[
      "S/N",
      "Product / Brand",
      "Unit",
      "Qty",
      "Rate (Rs.)",
      "Amount (Rs.)",
    ]],
    body: tableRows,
    theme: "grid",
    margin: {
      left: margin,
      right: margin,
      top: 14,
      bottom: footerReserve,
    },
    showHead: "everyPage",
    rowPageBreak: "avoid",
    styles: {
      font: "helvetica",
      fontSize: 8,
      cellPadding: 3,
      textColor: C.text,
      lineColor: C.grid,
      lineWidth: 0.2,
      overflow: "linebreak",
      valign: "middle",
      minCellHeight: 9,
    },
    headStyles: {
      fillColor: C.navy,
      textColor: C.white,
      fontStyle: "bold",
      minCellHeight: 10,
      halign: "center",
    },
    alternateRowStyles: {
      fillColor: C.lavender,
    },
    columnStyles: {
      0: { cellWidth: 10, halign: "center" },
      1: { cellWidth: 62, halign: "left" },
      2: { cellWidth: 16, halign: "center" },
      3: { cellWidth: 17, halign: "right" },
      4: { cellWidth: 32, halign: "right" },
      5: { cellWidth: 35, halign: "right" },
    },
    didParseCell(data) {
      if (data.section === "body" && data.column.index === 1) {
        data.cell.styles.fontSize = 8;
      }

      if (data.section === "body" && data.column.index === 5) {
        data.cell.styles.fontStyle = "bold";
        data.cell.styles.textColor = C.navy;
      }
    },
    didDrawPage(data) {
      // The first page already has its full header.
      // Subsequent pages receive a small continuation banner.
      if (data.pageNumber > 1) {
        pdf.setFillColor(...C.navy);
        pdf.rect(0, 0, pageWidth, 10, "F");

        pdf.setTextColor(...C.white);
        pdf.setFont("helvetica", "bold");
        pdf.setFontSize(8);
        pdf.text(
          textValue(shopProfile?.name, "SHREE DATTA FATAKA MART"),
          margin,
          6.5
        );

        pdf.setTextColor(...C.gold);
        pdf.text("INVOICE CONTINUED", rightEdge, 6.5, {
          align: "right",
        });
      }
    },
  });

  // Totals should stay together and never overlap the footer.
  let y = pdf.lastAutoTable.finalY + 8;

  const totalsHeight = 43;
  const statusHeight = 17;
  const gapAfterTotals = 5;
  const requiredSpace =
    totalsHeight + gapAfterTotals + statusHeight + 5;

  if (y + requiredSpace > pageHeight - footerReserve) {
    pdf.addPage();

    pdf.setFillColor(...C.bg);
    pdf.rect(0, 0, pageWidth, pageHeight, "F");

    pdf.setFillColor(...C.navy);
    pdf.rect(0, 0, pageWidth, 10, "F");

    pdf.setTextColor(...C.white);
    pdf.setFont("helvetica", "bold");
    pdf.setFontSize(8);
    pdf.text("INVOICE SUMMARY", margin, 6.5);

    y = 20;
  }

  // Colorful totals card.
  const totalsX = rightEdge - 86;
  const totalsWidth = 86;

  pdf.setFillColor(...C.white);
  pdf.setDrawColor(...C.grid);
  pdf.roundedRect(
    totalsX,
    y,
    totalsWidth,
    totalsHeight,
    2,
    2,
    "FD"
  );

  const totalRows = [
    {
      label: "Subtotal",
      value: money(subtotal),
      color: C.text,
      fill: C.white,
      height: 9,
    },
    {
      label: "Grand Total",
      value: money(total),
      color: C.white,
      fill: C.orange,
      height: 11,
    },
    {
      label: "Amount Paid",
      value: money(amountPaid),
      color: C.navy,
      fill: C.lightBlue,
      height: 9,
    },
    {
      label: "Balance Due",
      value: money(pendingAmount),
      color: C.crimson,
      fill: C.paleRed,
      height: 10,
    },
  ];

  let rowY = y;

  totalRows.forEach((row) => {
    pdf.setFillColor(...row.fill);
    pdf.rect(totalsX, rowY, totalsWidth, row.height, "F");

    pdf.setFont(
      "helvetica",
      row.label === "Grand Total" || row.label === "Balance Due"
        ? "bold"
        : "normal"
    );

    pdf.setFontSize(row.label === "Grand Total" ? 9 : 8);
    pdf.setTextColor(...row.color);

    pdf.text(row.label, totalsX + 4, rowY + row.height / 2 + 1.1);
    pdf.text(row.value, rightEdge - 4, rowY + row.height / 2 + 1.1, {
      align: "right",
    });

    rowY += row.height;
  });

  y += totalsHeight + gapAfterTotals;

  // Payment and bill status.
  const normalizedPaymentStatus = String(
    bill.paymentStatus || "—"
  ).toUpperCase();

  const isPaid = normalizedPaymentStatus === "PAID";

  const statusColor = isPaid
    ? C.green
    : pendingAmount > 0
      ? C.crimson
      : C.teal;

  pdf.setFillColor(...(isPaid ? C.paleGreen : C.paleOrange));
  pdf.setDrawColor(...statusColor);
  pdf.roundedRect(margin, y, contentWidth, statusHeight, 2, 2, "FD");

  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(8);
  pdf.setTextColor(...statusColor);

  pdf.text(
    `PAYMENT: ${normalizedPaymentStatus}`,
    margin + 4,
    y + 7
  );

  pdf.setTextColor(...C.text);
  pdf.text(
    `BILL: ${textValue(bill.billStatus, "—").toUpperCase()}`,
    margin + 4,
    y + 12.5
  );

  // Decorative Diwali message.
  y += statusHeight + 9;

  if (y + 13 < pageHeight - footerReserve) {
    drawDiya(pdf, margin + 8, y + 5, 0.8);
    drawFirework(pdf, rightEdge - 5, y + 5, 3, C.orange);

    pdf.setFont("helvetica", "bolditalic");
    pdf.setFontSize(13);
    pdf.setTextColor(...C.crimson);
    pdf.text("Happy Diwali!", pageWidth / 2 - 13, y + 5, {
      align: "center",
    });

    pdf.setFont("helvetica", "bold");
    pdf.setFontSize(8);
    pdf.setTextColor(...C.teal);
    pdf.text("Thank you for your purchase!", pageWidth / 2, y + 11, {
      align: "center",
    });
  }

  // Apply a consistent footer to every page, including table continuations.
  const pageCount = pdf.internal.getNumberOfPages();

  for (let page = 1; page <= pageCount; page += 1) {
    pdf.setPage(page);
    drawFooter(pdf, pageWidth, pageHeight, page, pageCount);
  }

  const safeBillNumber = String(bill.billNumber || "bill").replace(
    /[^a-zA-Z0-9_-]/g,
    "_"
  );

  pdf.save(`${safeBillNumber}.pdf`);
}