import sqlite3, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
conn = sqlite3.connect(r'E:\AIWorkspace\leave_manage\publish\data.db')
conn.text_factory = lambda x: x.decode('utf-8', 'replace')
cur = conn.cursor()

cur.execute("SELECT COUNT(*) FROM leave_applications la WHERE la.status='已审批' AND CAST(strftime('%Y', la.start_date) AS INTEGER)=2026 AND la.id NOT IN (SELECT application_id FROM leave_cancellations)")
print('Pending 2026:', cur.fetchone()[0])

cur.execute("SELECT COUNT(*) FROM leave_applications la WHERE la.status='已审批' AND la.id NOT IN (SELECT application_id FROM leave_cancellations)")
print('Pending all years:', cur.fetchone()[0])

cur.execute("SELECT COUNT(*) FROM leave_applications WHERE status='已审批'")
print('All approved:', cur.fetchone()[0])

cur.execute("SELECT COUNT(*) FROM leave_cancellations")
print('Cancellations:', cur.fetchone()[0])

conn.close()
