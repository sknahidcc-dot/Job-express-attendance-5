/**
 * ================================================================
 * Job Express Attendance - Google Apps Script Backend
 * ================================================================
 * Instructions:
 * 1. Open your Google Sheet.
 * 2. Create two tabs/sheets:
 *    Tab 1 name: "Students"
 *    Tab 2 name: "Attendance"
 * 3. Go to Extensions -> Apps Script.
 * 4. Paste this complete code into Code.gs and save.
 * 5. Run setupSheets() once if headers are not created.
 * 6. Click Deploy -> New deployment -> Select type: Web App.
 *    - Execute as: "Me"
 *    - Who has access: "Anyone" (CRITICAL: must be Anyone so Android app can communicate).
 * 7. Copy the Web App URL and paste it into the Job Express app Admin Panel or QR code!
 * ================================================================
 */

function setupSheets() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  
  // Setup Students tab
  let studentsSheet = ss.getSheetByName("Students");
  if (!studentsSheet) {
    studentsSheet = ss.insertSheet("Students");
  }
  if (studentsSheet.getLastRow() === 0) {
    studentsSheet.appendRow([
      "LID", "Name", "ShelfNo", "Phone", "BloodGroup", "Email", "Address", "PhotoURL", "Active", "CreatedAt"
    ]);
    studentsSheet.getRange(1, 1, 1, 10).setFontWeight("bold").setBackground("#D32F2F").setFontColor("#FFFFFF");
  }
  
  // Setup Attendance tab
  let attendanceSheet = ss.getSheetByName("Attendance");
  if (!attendanceSheet) {
    attendanceSheet = ss.insertSheet("Attendance");
  }
  if (attendanceSheet.getLastRow() === 0) {
    attendanceSheet.appendRow([
      "LID", "Name", "Date", "StartTime", "EndTime", "PhotoURL", "Status"
    ]);
    attendanceSheet.getRange(1, 1, 1, 7).setFontWeight("bold").setBackground("#D32F2F").setFontColor("#FFFFFF");
  }
}

function doGet(e) {
  return handleRequest(e);
}

function doPost(e) {
  return handleRequest(e);
}

function handleRequest(e) {
  const lock = LockService.getScriptLock();
  lock.tryLock(15000);
  
  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const studentsSheet = ss.getSheetByName("Students") || ss.insertSheet("Students");
    const attendanceSheet = ss.getSheetByName("Attendance") || ss.insertSheet("Attendance");
    
    let params = {};
    if (e && e.parameter) {
      params = Object.assign({}, e.parameter);
    }
    if (e && e.postData && e.postData.contents) {
      try {
        const body = JSON.parse(e.postData.contents);
        params = Object.assign(params, body);
      } catch (err) {
        // Fallback or ignore non-json body
      }
    }
    
    const action = params.action || "get_stats";
    
    // -------------------------------------------------------------
    // ACTION: get_students
    // -------------------------------------------------------------
    if (action === "get_students") {
      const data = studentsSheet.getDataRange().getValues();
      const students = [];
      if (data.length > 1) {
        for (let i = 1; i < data.length; i++) {
          const row = data[i];
          if (row[0]) {
            students.push({
              lid: String(row[0]).trim(),
              name: String(row[1] || ""),
              shelfNo: String(row[2] || ""),
              phone: String(row[3] || ""),
              bloodGroup: String(row[4] || ""),
              email: String(row[5] || ""),
              address: String(row[6] || ""),
              photoUrl: String(row[7] || ""),
              active: String(row[8] || "active").toLowerCase(),
              createdAt: String(row[9] || "")
            });
          }
        }
      }
      return jsonResponse({ success: true, students: students });
    }
    
    // -------------------------------------------------------------
    // ACTION: get_student (by LID)
    // -------------------------------------------------------------
    if (action === "get_student") {
      const targetLid = String(params.lid || "").trim().toLowerCase();
      const data = studentsSheet.getDataRange().getValues();
      for (let i = 1; i < data.length; i++) {
        const row = data[i];
        if (String(row[0]).trim().toLowerCase() === targetLid) {
          const student = {
            lid: String(row[0]).trim(),
            name: String(row[1] || ""),
            shelfNo: String(row[2] || ""),
            phone: String(row[3] || ""),
            bloodGroup: String(row[4] || ""),
            email: String(row[5] || ""),
            address: String(row[6] || ""),
            photoUrl: String(row[7] || ""),
            active: String(row[8] || "active").toLowerCase(),
            createdAt: String(row[9] || "")
          };
          return jsonResponse({ success: true, student: student });
        }
      }
      return jsonResponse({ success: false, message: "Student not found" });
    }
    
    // -------------------------------------------------------------
    // ACTION: add_student
    // -------------------------------------------------------------
    if (action === "add_student") {
      const lid = String(params.lid || "").trim();
      const name = String(params.name || "").trim();
      const shelfNo = String(params.shelfNo || "").trim();
      const phone = String(params.phone || "").trim();
      const bloodGroup = String(params.bloodGroup || "A+").trim();
      const email = String(params.email || "").trim();
      const address = String(params.address || "").trim();
      const photoUrl = String(params.photoUrl || "").trim();
      const active = String(params.active || "active").trim().toLowerCase();
      const createdAt = params.createdAt || new Date().toISOString();
      
      if (!lid || !name) {
        return jsonResponse({ success: false, message: "LID and Name are required" });
      }
      
      // Check if LID already exists
      const data = studentsSheet.getDataRange().getValues();
      for (let i = 1; i < data.length; i++) {
        if (String(data[i][0]).trim().toLowerCase() === lid.toLowerCase()) {
          // Update existing student
          studentsSheet.getRange(i + 1, 1, 1, 10).setValues([[
            lid, name, shelfNo, phone, bloodGroup, email, address, photoUrl, active, data[i][9] || createdAt
          ]]);
          return jsonResponse({ success: true, message: "Student updated successfully", lid: lid });
        }
      }
      
      // Append new student
      studentsSheet.appendRow([
        lid, name, shelfNo, phone, bloodGroup, email, address, photoUrl, active, createdAt
      ]);
      return jsonResponse({ success: true, message: "Student added successfully", lid: lid });
    }
    
    // -------------------------------------------------------------
    // ACTION: delete_student
    // -------------------------------------------------------------
    if (action === "delete_student") {
      const targetLid = String(params.lid || "").trim().toLowerCase();
      const data = studentsSheet.getDataRange().getValues();
      for (let i = 1; i < data.length; i++) {
        if (String(data[i][0]).trim().toLowerCase() === targetLid) {
          studentsSheet.deleteRow(i + 1);
          return jsonResponse({ success: true, message: "Student deleted successfully" });
        }
      }
      return jsonResponse({ success: false, message: "Student not found to delete" });
    }
    
    // -------------------------------------------------------------
    // ACTION: record_attendance
    // -------------------------------------------------------------
    if (action === "record_attendance") {
      const targetLid = String(params.lid || "").trim().toLowerCase();
      if (!targetLid) {
        return jsonResponse({ success: false, message: "LID is required" });
      }
      
      // Verify in Students sheet
      const sData = studentsSheet.getDataRange().getValues();
      let matchedStudent = null;
      for (let i = 1; i < sData.length; i++) {
        if (String(sData[i][0]).trim().toLowerCase() === targetLid) {
          matchedStudent = {
            lid: String(sData[i][0]).trim(),
            name: String(sData[i][1] || ""),
            shelfNo: String(sData[i][2] || ""),
            phone: String(sData[i][3] || ""),
            bloodGroup: String(sData[i][4] || ""),
            email: String(sData[i][5] || ""),
            address: String(sData[i][6] || ""),
            photoUrl: String(sData[i][7] || ""),
            active: String(sData[i][8] || "active").toLowerCase()
          };
          break;
        }
      }
      
      if (!matchedStudent) {
        return jsonResponse({ success: false, message: "স্টুডেন্ট আইডি সঠিক নয়। অনুগ্রহ করে 01351835060 এ যোগাযোগ করুন।" });
      }
      
      if (matchedStudent.active !== "active") {
        return jsonResponse({ success: false, message: "আপনার সদস্যপদ নিষ্ক্রিয় (Inactive)। অনুগ্রহ করে 01351835060 এ যোগাযোগ করুন।" });
      }
      
      const now = new Date();
      const timeZone = Session.getScriptTimeZone() || "Asia/Dhaka";
      const todayDate = Utilities.formatDate(now, timeZone, "yyyy-MM-dd");
      const currentTime = Utilities.formatDate(now, timeZone, "hh:mm a");
      
      // Check Attendance Sheet for today's entry
      const aData = attendanceSheet.getDataRange().getValues();
      let existingRowIndex = -1;
      let existingRecord = null;
      
      for (let i = 1; i < aData.length; i++) {
        const rowLid = String(aData[i][0]).trim().toLowerCase();
        const rowDate = Utilities.formatDate(new Date(aData[i][2]), timeZone, "yyyy-MM-dd");
        if (rowLid === targetLid && rowDate === todayDate) {
          existingRowIndex = i + 1;
          existingRecord = {
            lid: aData[i][0],
            name: aData[i][1],
            date: rowDate,
            startTime: aData[i][3],
            endTime: aData[i][4],
            photoUrl: aData[i][5],
            status: aData[i][6]
          };
          break;
        }
      }
      
      if (existingRowIndex === -1) {
        // First scan today -> ENTRY (প্রবেশ সময়)
        attendanceSheet.appendRow([
          matchedStudent.lid,
          matchedStudent.name,
          todayDate,
          currentTime,
          "",
          matchedStudent.photoUrl || "",
          "Inside"
        ]);
        return jsonResponse({
          success: true,
          type: "ENTRY",
          time: currentTime,
          date: todayDate,
          student: matchedStudent,
          message: "স্বাগতম! আপনার প্রবেশ সময় " + currentTime + " সফলভাবে রেকর্ড করা হয়েছে।"
        });
      } else {
        // Second scan today -> EXIT (বের হওয়ার সময়)
        // Update EndTime and Status
        attendanceSheet.getRange(existingRowIndex, 5).setValue(currentTime);
        attendanceSheet.getRange(existingRowIndex, 7).setValue("Left");
        
        return jsonResponse({
          success: true,
          type: "EXIT",
          time: currentTime,
          date: todayDate,
          startTime: existingRecord.startTime,
          student: matchedStudent,
          message: "বিদায়! আপনার বের হওয়ার সময় " + currentTime + " সফলভাবে রেকর্ড করা হয়েছে।"
        });
      }
    }
    
    // -------------------------------------------------------------
    // ACTION: get_attendance
    // -------------------------------------------------------------
    if (action === "get_attendance") {
      const aData = attendanceSheet.getDataRange().getValues();
      const records = [];
      const timeZone = Session.getScriptTimeZone() || "Asia/Dhaka";
      if (aData.length > 1) {
        for (let i = 1; i < aData.length; i++) {
          const row = aData[i];
          if (row[0]) {
            let rowDateStr = "";
            try {
              rowDateStr = Utilities.formatDate(new Date(row[2]), timeZone, "yyyy-MM-dd");
            } catch (dErr) {
              rowDateStr = String(row[2] || "");
            }
            records.push({
              lid: String(row[0]).trim(),
              name: String(row[1] || ""),
              date: rowDateStr,
              startTime: String(row[3] || ""),
              endTime: String(row[4] || ""),
              photoUrl: String(row[5] || ""),
              status: String(row[6] || "Inside")
            });
          }
        }
      }
      return jsonResponse({ success: true, records: records.reverse() });
    }
    
    // -------------------------------------------------------------
    // ACTION: get_stats
    // -------------------------------------------------------------
    const sData = studentsSheet.getDataRange().getValues();
    let totalStudents = 0;
    let activeSeats = 0;
    if (sData.length > 1) {
      for (let i = 1; i < sData.length; i++) {
        if (sData[i][0]) {
          totalStudents++;
          const status = String(sData[i][8] || "active").toLowerCase();
          if (status === "active") {
            activeSeats++;
          }
        }
      }
    }
    
    const aData = attendanceSheet.getDataRange().getValues();
    const timeZone = Session.getScriptTimeZone() || "Asia/Dhaka";
    const todayDate = Utilities.formatDate(new Date(), timeZone, "yyyy-MM-dd");
    let todayTotalPresent = 0;
    let todayCurrentlyInside = 0;
    
    if (aData.length > 1) {
      for (let i = 1; i < aData.length; i++) {
        if (aData[i][0]) {
          let rowDate = "";
          try {
            rowDate = Utilities.formatDate(new Date(aData[i][2]), timeZone, "yyyy-MM-dd");
          } catch(e) {
            rowDate = String(aData[i][2]);
          }
          if (rowDate === todayDate) {
            todayTotalPresent++;
            const status = String(aData[i][6] || "").trim();
            if (status.toLowerCase() === "inside") {
              todayCurrentlyInside++;
            }
          }
        }
      }
    }
    
    const availableSeats = Math.max(0, activeSeats - todayCurrentlyInside);
    const todayAbsent = Math.max(0, activeSeats - todayTotalPresent);
    
    return jsonResponse({
      success: true,
      stats: {
        totalStudents: totalStudents,
        totalSeats: activeSeats,
        activeSeats: activeSeats,
        todayTotalPresent: todayTotalPresent,
        todayCurrentlyInside: todayCurrentlyInside,
        availableSeats: availableSeats,
        todayAbsent: todayAbsent,
        date: todayDate
      }
    });
    
  } catch (error) {
    return jsonResponse({ success: false, error: error.toString() });
  } finally {
    lock.releaseLock();
  }
}

function jsonResponse(data) {
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}
